import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Users, Loader2, AlertTriangle, Database, RefreshCw, Download } from 'lucide-react';
import { characterApi } from '../services/api.ts';
import type { HistoricalCharacter } from '../types';
import { Link } from 'react-router-dom';

export function CharacterSelection() {
  const [search, setSearch] = useState('');

  const { data: characters, isLoading, error, refetch } = useQuery({
    queryKey: ['characters', search],
    queryFn: () => search ? characterApi.search(search) : characterApi.getAll(),
    retry: 1,
  });

  const queryClient = useQueryClient();
  const [importError, setImportError] = useState<string | null>(null);

  const importMutation = useMutation({
    mutationFn: (name: string) => characterApi.importFromWikipedia(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['characters'] });
      setImportError(null);
    },
    onError: (err: Error) => {
      setImportError(err.message || 'Nie udało się zaimportować z Wikipedia');
    },
  });

  const hasError = error !== null;
  const isEmpty = !isLoading && !hasError && (!characters || characters.length === 0);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="container mx-auto px-4 py-12">
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold text-white mb-4">
            Historia AI
          </h1>
          <p className="text-xl text-slate-300 max-w-2xl mx-auto">
            Porozmawiaj z wybitnymi postaciami historycznymi. Poznaj historię w interaktywny sposób.
          </p>
        </div>

        <div className="max-w-xl mx-auto mb-12">
          <div className="relative flex gap-2">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
            <input
              type="text"
              placeholder="Wyszukaj postać historyczną..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 pl-12 pr-4 py-4 bg-slate-800/50 border border-slate-700 rounded-2xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
            />
            <button
              onClick={() => {
                if (search.trim()) {
                  importMutation.mutate(search.trim());
                }
              }}
              disabled={!search.trim() || importMutation.isPending}
              className="px-4 py-4 bg-blue-500 hover:bg-blue-600 disabled:bg-slate-700 disabled:cursor-not-allowed text-white font-medium rounded-2xl transition-colors flex items-center gap-2"
            >
              {importMutation.isPending ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <Download className="w-5 h-5" />
              )}
              <span className="hidden sm:inline">Dodaj z Wikipedia</span>
            </button>
          </div>
          {importError && (
            <p className="text-red-400 text-sm mt-2">{importError}</p>
          )}
          {importMutation.isSuccess && (
            <p className="text-green-400 text-sm mt-2">Zaimportowano pomyślnie!</p>
          )}
        </div>

        {hasError && (
          <div className="max-w-2xl mx-auto mb-8">
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-red-400 font-medium">Nie udało połączyć się z serwerem</p>
                <p className="text-red-300/70 text-sm mt-1">
                  Upewnij się, że backend jest uruchomiony na porcie 8080.
                </p>
                <button
                  onClick={() => refetch()}
                  className="mt-2 flex items-center gap-2 text-sm text-red-400 hover:text-red-300"
                >
                  <RefreshCw className="w-4 h-4" />
                  Spróbuj ponownie
                </button>
              </div>
            </div>
          </div>
        )}

        {isEmpty && !hasError && !search && (
          <div className="max-w-2xl mx-auto mb-8">
            <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 flex items-start gap-3">
              <Database className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-amber-400 font-medium">Brak postaci w bazie danych</p>
                <p className="text-amber-300/70 text-sm mt-1">
                  Wyszukaj i dodaj postać z Wikipedia powyżej, aby rozpocząć.
                </p>
              </div>
            </div>
          </div>
        )}

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-10 h-10 text-amber-500 animate-spin" />
          </div>
        ) : characters && characters.length > 0 ? (
          <>
            <div className="flex items-center gap-3 mb-8">
              <Users className="w-6 h-6 text-amber-500" />
              <h2 className="text-2xl font-semibold text-white">Wybierz postać</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {characters.map((character) => (
                <CharacterCard key={character.id} character={character} />
              ))}
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <p className="text-slate-400 text-lg mb-2">
              Brak postaci do wyświetlenia
            </p>
            <p className="text-slate-500 text-sm">
              Wyszukaj i dodaj postać z Wikipedia powyżej
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

function CharacterCard({ character }: { character: HistoricalCharacter }) {
  const years = character.birthYear && character.deathYear 
    ? `${character.birthYear} - ${character.deathYear}` 
    : character.birthYear 
      ? `ur. ${character.birthYear}` 
      : '';

  return (
    <Link
      to={`/chat/${character.id}`}
      className="group bg-slate-800/50 backdrop-blur-sm border border-slate-700 rounded-2xl overflow-hidden hover:border-amber-500/50 hover:shadow-xl hover:shadow-amber-500/10 transition-all duration-300"
    >
      <div className="aspect-[4/3] overflow-hidden bg-gradient-to-br from-amber-500/20 to-slate-700">
        {character.imageUrl ? (
          <img
            src={character.imageUrl}
            alt={character.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Users className="w-16 h-16 text-slate-600" />
          </div>
        )}
      </div>
      <div className="p-5">
        <h3 className="text-xl font-bold text-white mb-1 group-hover:text-amber-400 transition-colors">
          {character.name}
        </h3>
        {years && (
          <p className="text-sm text-slate-400 mb-2">{years}</p>
        )}
        <div className="flex gap-2 mb-3">
          <span className="px-2 py-1 bg-amber-500/20 text-amber-400 text-xs rounded-full">
            {character.era}
          </span>
          <span className="px-2 py-1 bg-slate-700 text-slate-300 text-xs rounded-full">
            {character.nationality}
          </span>
        </div>
        <p className="text-sm text-slate-400 line-clamp-2">
          {character.biography}
        </p>
      </div>
    </Link>
  );
}
