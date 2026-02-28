import axios from 'axios';
import type { HistoricalCharacter, FactCheckResult, ChatRequest, ChatResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const characterApi = {
  getAll: async (): Promise<HistoricalCharacter[]> => {
    const response = await apiClient.get<HistoricalCharacter[]>('/characters');
    return response.data;
  },

  getById: async (id: string): Promise<HistoricalCharacter> => {
    const response = await apiClient.get<HistoricalCharacter>(`/characters/${id}`);
    return response.data;
  },

  search: async (query: string): Promise<HistoricalCharacter[]> => {
    const response = await apiClient.get<HistoricalCharacter[]>('/characters/search', {
      params: { q: query },
    });
    return response.data;
  },

  create: async (character: Omit<HistoricalCharacter, 'id' | 'createdAt' | 'updatedAt'>): Promise<HistoricalCharacter> => {
    const response = await apiClient.post<HistoricalCharacter>('/characters', character);
    return response.data;
  },

  importFromWikipedia: async (name: string): Promise<HistoricalCharacter> => {
    const response = await apiClient.post<HistoricalCharacter>('/characters/import', null, {
      params: { name },
    });
    return response.data;
  },
};

export const chatApi = {
  send: async (request: ChatRequest): Promise<ChatResponse> => {
    const response = await apiClient.post<ChatResponse>('/chat', request);
    return response.data;
  },

  getHistory: async (conversationId: string): Promise<{ messages: Array<{ role: string; content: string; timestamp: string }> }> => {
    const response = await apiClient.get(`/chat/${conversationId}/history`);
    return response.data;
  },
};

export const factCheckApi = {
  verify: async (message: string, characterName: string, characterContext?: string): Promise<FactCheckResult[]> => {
    const response = await apiClient.post<FactCheckResult[]>('/factcheck', {
      message,
      characterName,
      characterContext,
    });
    return response.data;
  },

  verifyStream: () => {
    // This function is not used - EventSource is handled directly in Chat.tsx
    throw new Error('verifyStream is not implemented - use EventSource directly in Chat.tsx');
  },
};
