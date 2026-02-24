export interface HistoricalCharacter {
  id: string;
  name: string;
  birthYear: number | null;
  deathYear: number | null;
  biography: string;
  imageUrl: string | null;
  era: string;
  nationality: string;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
}

export interface Conversation {
  id: string;
  characterId: string;
  userId: string;
  title: string;
  messages: Message[];
  createdAt: string;
  updatedAt: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
}

export interface ChatResponse {
  message: string;
  conversationId: string;
  characterId: string;
}

export interface FactCheckResult {
  claim: string;
  verification: 'VERIFIED' | 'FALSE' | 'UNVERIFIABLE';
  source: string;
  explanation: string;
  confidence: number;
}

export interface FactCheckResponse {
  results: FactCheckResult[];
}
