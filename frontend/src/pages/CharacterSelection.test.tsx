import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { CharacterSelection } from './CharacterSelection';
import { characterApi } from '../services/api';

vi.mock('../services/api', () => ({
  characterApi: {
    getAll: vi.fn(),
    search: vi.fn(),
    importFromWikipedia: vi.fn(),
  },
}));

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CharacterSelection />
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe('CharacterSelection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(characterApi.search).mockResolvedValue([]);
  });

  it('renders empty state when no characters exist', async () => {
    vi.mocked(characterApi.getAll).mockResolvedValueOnce([]);

    renderPage();

    expect(await screen.findByText('Brak postaci w bazie danych')).toBeInTheDocument();
  });

  it('shows error banner when API fails and allows retry', async () => {
    const error = new Error('Network error');
    vi.mocked(characterApi.getAll).mockRejectedValue(error);

    renderPage();

    expect(
      await screen.findByText(/Nie udało połączyć się z serwerem/, {}, { timeout: 3000 })
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText('Spróbuj ponownie'));
  });

  it('renders character cards when data is returned', async () => {
    vi.mocked(characterApi.getAll).mockResolvedValueOnce([
      {
        id: '1',
        name: 'Mikołaj Kopernik',
        birthYear: 1473,
        deathYear: 1543,
        biography: 'Astronom',
        imageUrl: null,
        era: 'Renesans',
        nationality: 'Polska',
        createdAt: '',
        updatedAt: '',
      },
    ]);

    renderPage();

    expect(await screen.findByText('Mikołaj Kopernik')).toBeInTheDocument();
    expect(screen.getByText('Renesans')).toBeInTheDocument();
    expect(screen.getByText('Polska')).toBeInTheDocument();
  });

  it('calls import when clicking add from Wikipedia', async () => {
    vi.mocked(characterApi.getAll).mockResolvedValueOnce([]);
    vi.mocked(characterApi.importFromWikipedia).mockResolvedValueOnce({
      id: '1',
      name: 'Test',
      birthYear: null,
      deathYear: null,
      biography: '',
      imageUrl: null,
      era: 'XX wiek',
      nationality: 'Polska',
      createdAt: '',
      updatedAt: '',
    });

    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Wyszukaj postać historyczną...'), {
      target: { value: 'Test' },
    });
    fireEvent.click(screen.getByText('Dodaj z Wikipedia'));

    await waitFor(() => {
      expect(characterApi.importFromWikipedia).toHaveBeenCalledWith('Test');
    });
  });
});
