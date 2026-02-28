import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CharacterSelection } from './pages/CharacterSelection';
import { Chat } from './pages/Chat';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<CharacterSelection />} />
          <Route path="/chat/:characterId" element={<Chat />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
