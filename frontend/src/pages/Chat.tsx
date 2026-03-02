import { useState, useRef, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { ArrowLeft, Send, Shield, Loader2, CheckCircle, XCircle, HelpCircle, MessageCircle, Bot, ChevronUp, ChevronDown } from 'lucide-react';
import { characterApi } from '../services/api';
import { formatStreamText, parseFinalEvent, parseStructuredResult } from '../utils/factcheck';
import type { FactCheckResult } from '../types';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isStreaming?: boolean;
}

interface VerificationMessage {
  id: string;
  claim: string;
  results: FactCheckResult[];
  timestamp: Date;
  isStreaming?: boolean;
  streamingContent?: string;
}

type TabType = 'chat' | 'verify';

export function Chat() {
  const { characterId } = useParams<{ characterId: string }>();
  const [chatMessages, setChatMessages] = useState<Message[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [chatConversationId, setChatConversationId] = useState<string | undefined>(undefined);
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [verifications, setVerifications] = useState<VerificationMessage[]>([]);
  const [verificationInput, setVerificationInput] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>('chat');
  const [showSidebar, setShowSidebar] = useState(true);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const verificationEndRef = useRef<HTMLDivElement>(null);

  const { data: character, isLoading, isError } = useQuery({
    queryKey: ['character', characterId],
    queryFn: () => characterApi.getById(characterId!),
    retry: 1,
    enabled: !!characterId,
  });

  const activeCharacter = character;

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  useEffect(() => {
    verificationEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [verifications]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <Loader2 className="w-8 h-8 text-amber-500 animate-spin" />
      </div>
    );
  }

  if (isError || !activeCharacter) {
    return (
      <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4">
        <XCircle className="w-12 h-12 text-red-500 mb-4" />
        <p className="text-slate-400 mb-4 text-center">Nie udało się załadować danych postaci</p>
        <Link to="/" className="text-amber-500 hover:text-amber-400">Wróć do strony głównej</Link>
      </div>
    );
  }

  const handleSendChat = () => {
    if (!chatInput.trim() || !activeCharacter || isChatLoading) return;

    const message = chatInput.trim();
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: message,
      timestamp: new Date(),
    };
    const assistantId = (Date.now() + 1).toString();

    setChatMessages(prev => [...prev, userMessage, {
      id: assistantId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true,
    }]);
    setChatInput('');
    setIsChatLoading(true);

    try {
      const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
      const urlParams = new URLSearchParams({
        message,
        characterId: activeCharacter.id,
        ...(chatConversationId && { conversationId: chatConversationId }),
      });
      const eventSource = new EventSource(`${baseUrl}/chat/stream?${urlParams.toString()}`);
      let currentContent = '';
      let finished = false;

      const finalizeMessage = (content: string) => {
        setChatMessages(prev =>
          prev.map(m => m.id === assistantId ? { ...m, content, isStreaming: false } : m)
        );
      };

      eventSource.addEventListener('chunk', (event) => {
        if (finished) return;
        const data = (event as MessageEvent).data;
        if (!data || data.trim() === '') return;
        currentContent += data;
        finalizeMessage(currentContent);
      });

      eventSource.addEventListener('final', (event) => {
        if (finished) return;
        finished = true;
        const data = (event as MessageEvent).data;
        try {
          const parsed = JSON.parse(data) as { message?: string; conversationId?: string };
          if (parsed.conversationId) {
            setChatConversationId(parsed.conversationId);
          }
          const finalText = parsed.message?.trim() || currentContent.trim();
          finalizeMessage(finalText || 'Brak odpowiedzi');
        } catch {
          finalizeMessage(currentContent.trim() || 'Brak odpowiedzi');
        }
        eventSource.close();
        setIsChatLoading(false);
      });

      eventSource.addEventListener('complete', () => {
        if (finished) return;
        finished = true;
        finalizeMessage(currentContent.trim() || 'Brak odpowiedzi');
        eventSource.close();
        setIsChatLoading(false);
      });

      eventSource.addEventListener('error', (event) => {
        const backendMessage = (event as MessageEvent).data;
        if (finished) return;
        finished = true;
        finalizeMessage(backendMessage?.trim() || currentContent.trim() || 'Nie udało się uzyskać odpowiedzi. Spróbuj ponownie za chwilę.');
        eventSource.close();
        setIsChatLoading(false);
      });

      eventSource.onerror = async () => {
        if (finished) return;
        eventSource.close();
        try {
          const response = await axios.post<{ message: string; conversationId: string }>(
            `${baseUrl}/chat`,
            {
              message,
              characterId: activeCharacter.id,
              conversationId: chatConversationId,
            },
            { timeout: 180000 }
          );
          setChatConversationId(response.data.conversationId);
          finalizeMessage(response.data.message || currentContent || 'Brak odpowiedzi');
        } catch (error) {
          let fallbackMessage = 'Nie udało się uzyskać odpowiedzi. Spróbuj ponownie za chwilę.';
          if (axios.isAxiosError(error)) {
            const msg = (error.response?.data as { message?: string } | undefined)?.message;
            if (msg && msg.trim()) {
              fallbackMessage = msg;
            } else if (error.response?.status === 504) {
              fallbackMessage = 'Model AI odpowiada zbyt długo. Spróbuj ponownie za chwilę.';
            } else if (error.response?.status === 503) {
              fallbackMessage = 'Usługa modelu AI jest chwilowo niedostępna. Spróbuj ponownie za chwilę.';
            }
          }
          finalizeMessage(currentContent.trim() || fallbackMessage);
        } finally {
          finished = true;
          setIsChatLoading(false);
        }
      };
    } catch (error) {
      console.error('Chat streaming setup failed:', error);
      setChatMessages(prev =>
        prev.map(m =>
          m.id === assistantId
            ? { ...m, content: 'Nie udało się uzyskać odpowiedzi. Spróbuj ponownie za chwilę.', isStreaming: false }
            : m
        )
      );
      setIsChatLoading(false);
    }
  };

  const handleVerify = async () => {
    if (!verificationInput.trim() || isVerifying || !activeCharacter) return;

    setIsVerifying(true);
    const messageId = Date.now().toString();
    const claim = verificationInput.trim();
    let currentContent = '';
    let finalized = false;

    setVerifications(prev => [...prev, {
      id: messageId,
      claim,
      results: [],
      timestamp: new Date(),
      isStreaming: true,
      streamingContent: ''
    }]);

    try {
      const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
      const url = `${baseUrl}/factcheck/stream`;
      const maxReconnectionAttempts = 3;
      const reconnectionDelay = 2000;
      const urlParams = new URLSearchParams({
        message: claim,
        characterName: activeCharacter.name,
        ...(activeCharacter.biography && { characterContext: activeCharacter.biography })
      });

      const finalizeStructured = (result: FactCheckResult) => {
        if (finalized) return;
        finalized = true;
        setIsVerifying(false);
        setVerificationInput('');
        setVerifications(prev =>
          prev.map(v =>
            v.id === messageId
              ? {
                  ...v,
                  isStreaming: false,
                  results: [result],
                }
              : v
          )
        );
      };

      const finalizeFromText = () => {
        const parsed = parseStructuredResult(currentContent);
        finalizeStructured({
          verification: parsed?.verification ?? 'UNVERIFIABLE',
          confidence: parsed?.confidence ?? 0.5,
          explanation: parsed?.explanation || currentContent || 'Weryfikacja zakończona',
          source: parsed?.source ?? 'AI',
        });
      };

      const streamWithRetry = (attempt: number) => {
        const eventSource = new EventSource(url + '?' + urlParams.toString());

        const appendChunk = (data: string) => {
          if (data.trim() === '' || finalized) return;
          currentContent += data;
          setVerifications(prev =>
            prev.map(v =>
              v.id === messageId
                ? { ...v, streamingContent: formatStreamText(currentContent) }
                : v
            )
          );
        };

        eventSource.onmessage = (event) => {
          if (event.data?.startsWith('Error:')) {
            eventSource.close();
            finalizeFromText();
            return;
          }
          appendChunk(event.data);
        };

        eventSource.addEventListener('chunk', (event) => {
          appendChunk((event as MessageEvent).data);
        });

        eventSource.addEventListener('final', (event) => {
          if (finalized) return;
          const data = (event as MessageEvent).data;
          const parsedResult = parseFinalEvent(data);
          if (parsedResult) {
            currentContent = parsedResult.explanation || currentContent;
            eventSource.close();
            finalizeStructured(parsedResult);
            return;
          }
          currentContent = data;
          eventSource.close();
          finalizeFromText();
        });

        eventSource.addEventListener('complete', () => {
          eventSource.close();
          finalizeFromText();
        });

        eventSource.onerror = () => {
          eventSource.close();
          if (finalized) return;
          if (attempt < maxReconnectionAttempts) {
            setTimeout(() => {
              streamWithRetry(attempt + 1);
            }, reconnectionDelay);
            return;
          }
          finalizeStructured({
            verification: 'UNVERIFIABLE',
            confidence: 0.5,
            explanation: currentContent || 'Weryfikacja nie powiodła się',
            source: 'AI',
          });
        };
      };

      streamWithRetry(0);
    } catch (error) {
      console.error('Verification failed:', error);
      setIsVerifying(false);
    }
  };

  const getVerificationIcon = (verification: string) => {
    switch (verification) {
      case 'VERIFIED':
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'FALSE':
        return <XCircle className="w-4 h-4 text-red-500" />;
      default:
        return <HelpCircle className="w-4 h-4 text-yellow-500" />;
    }
  };

  const getVerificationLabel = (verification: string) => {
    switch (verification) {
      case 'VERIFIED':
        return 'Prawda';
      case 'FALSE':
        return 'Fałsz';
      case 'PARTIAL':
        return 'Częściowo';
      default:
        return 'Nieweryfikowalne';
    }
  };

  const getVerificationColor = (verification: string) => {
    switch (verification) {
      case 'VERIFIED':
        return 'bg-green-500/20 border-green-500/30';
      case 'FALSE':
        return 'bg-red-500/20 border-red-500/30';
      case 'PARTIAL':
        return 'bg-yellow-500/20 border-yellow-500/30';
      default:
        return 'bg-slate-700/50 border-slate-600';
    }
  };

  return (
    <div className="h-screen overflow-hidden bg-slate-900 flex flex-col lg:flex-row">
      {/* Sidebar - character info */}
      <div className={`${showSidebar ? 'h-auto' : 'h-14'} lg:h-auto lg:w-72 bg-slate-800 lg:border-r border-slate-700 flex flex-col transition-all duration-300`}>
        <div className="flex items-center justify-between p-3 lg:p-4 border-b border-slate-700">
          <Link
            to="/"
            className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span className="hidden sm:inline">Wróć</span>
          </Link>
          <button 
            onClick={() => setShowSidebar(!showSidebar)}
            className="lg:hidden p-2 text-slate-400 hover:text-white"
          >
            {showSidebar ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>
        </div>

        {showSidebar && (
          <div className="p-3 lg:p-4 flex flex-col">
            <div className="flex items-center gap-3 mb-3">
              {activeCharacter.imageUrl ? (
                <img
                  src={activeCharacter.imageUrl}
                  alt={activeCharacter.name}
                  className="w-10 h-10 lg:w-12 lg:h-12 rounded-full object-cover border-2 border-amber-500"
                />
              ) : (
                <div className="w-10 h-10 lg:w-12 lg:h-12 rounded-full bg-slate-700 flex items-center justify-center">
                  <span className="text-lg lg:text-xl font-bold text-amber-500">
                    {activeCharacter.name[0]}
                  </span>
                </div>
              )}
              <div>
                <h2 className="font-bold text-white text-sm lg:text-base">{activeCharacter.name}</h2>
                <p className="text-xs text-slate-400">{activeCharacter.era}</p>
              </div>
            </div>

            <p className="text-xs lg:text-sm text-slate-400">
              {activeCharacter.biography}
            </p>
          </div>
        )}
      </div>

      {/* Main content */}
      <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
        {/* Tabs - mobile */}
        <div className="lg:hidden flex border-b border-slate-700">
          <button
            onClick={() => setActiveTab('chat')}
            className={`flex-1 flex items-center justify-center gap-2 py-3 text-sm font-medium transition-colors ${
              activeTab === 'chat' 
                ? 'text-blue-400 border-b-2 border-blue-400 bg-blue-500/10' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <MessageCircle className="w-4 h-4" />
            Chat
          </button>
          <button
            onClick={() => setActiveTab('verify')}
            className={`flex-1 flex items-center justify-center gap-2 py-3 text-sm font-medium transition-colors ${
              activeTab === 'verify' 
                ? 'text-amber-400 border-b-2 border-amber-400 bg-amber-500/10' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Shield className="w-4 h-4" />
            Weryfikacja
          </button>
        </div>

        {/* Desktop headers */}
        <div className="hidden lg:flex">
          <div className="flex-1 p-4 border-b border-slate-700 flex items-center gap-2">
            <MessageCircle className="w-5 h-5 text-blue-400" />
            <h3 className="font-semibold text-white">Czat z {activeCharacter.name}</h3>
          </div>
          <div className="flex-1 p-4 border-b border-slate-700 flex items-center gap-2">
            <Shield className="w-5 h-5 text-amber-400" />
            <h3 className="font-semibold text-white">Weryfikacja faktów</h3>
          </div>
        </div>

        {/* Content area */}
        <div className="flex-1 min-h-0 flex flex-col lg:flex-row overflow-hidden">
          {/* Chat panel */}
          <div className={`flex-1 flex flex-col ${activeTab !== 'chat' ? 'hidden lg:flex' : 'flex'} border-r border-slate-700`}>
            <div className="flex-1 overflow-y-auto p-3 lg:p-4 space-y-3 lg:space-y-4">
              {chatMessages.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-center p-4">
                  <Bot className="w-10 lg:w-12 h-10 lg:h-12 text-slate-600 mb-3" />
                  <p className="text-slate-400 text-sm">
                    Zadaj pytanie postaci historycznej.<br/>Odpowiedzi używają opisu i cytatów, jeśli są dostępne.
                  </p>
                </div>
              ) : (
                chatMessages.map((message) => (
                  <div
                    key={message.id}
                    className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[85%] lg:max-w-[80%] p-2 lg:p-3 rounded-xl lg:rounded-2xl ${
                        message.role === 'user'
                          ? 'bg-blue-500 text-white'
                          : 'bg-slate-800 text-slate-100'
                      }`}
                    >
                      {message.isStreaming && !message.content ? (
                        <div className="flex items-center gap-2">
                          <Loader2 className="w-4 h-4 text-blue-400 animate-spin" />
                          <p className="text-sm">Postać odpowiada...</p>
                        </div>
                      ) : (
                        <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                      )}
                    </div>
                  </div>
                ))
              )}
              <div ref={chatEndRef} />
            </div>

            <div className="p-3 lg:p-4 border-t border-slate-700">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSendChat()}
                  placeholder="Napisz wiadomość do postaci..."
                  disabled={isChatLoading}
                  className="flex-1 px-3 lg:px-4 py-2 lg:py-3 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={handleSendChat}
                  disabled={isChatLoading || !chatInput.trim()}
                  className="px-3 lg:px-4 py-2 lg:py-3 bg-blue-500 hover:bg-blue-600 disabled:bg-slate-700 disabled:cursor-not-allowed text-white rounded-xl transition-colors"
                >
                  {isChatLoading ? (
                    <Loader2 className="w-4 lg:w-5 h-4 lg:h-5 animate-spin" />
                  ) : (
                    <Send className="w-4 lg:w-5 h-4 lg:h-5" />
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Fact-check panel */}
          <div className={`flex-1 flex flex-col ${activeTab !== 'verify' ? 'hidden lg:flex' : 'flex'}`}>
            <div className="flex-1 overflow-y-auto p-3 lg:p-4 space-y-3 lg:space-y-4">
              {verifications.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-center p-4">
                  <Shield className="w-10 lg:w-12 h-10 lg:h-12 text-amber-500/50 mb-3" />
                  <p className="text-slate-400 text-sm max-w-xs">
                    Wpisz stwierdzenie np. "{activeCharacter.name} urodził się w {activeCharacter.birthYear} roku"
                  </p>
                </div>
              ) : (
                verifications.map((verification) => (
                  <div key={verification.id} className="space-y-2 lg:space-y-3">
                    <div className="flex justify-end">
                      <div className="max-w-[85%] p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-amber-500 text-white">
                        <p className="text-sm">{verification.claim}</p>
                      </div>
                    </div>

                    {verification.isStreaming && verification.streamingContent && (
                      <div className="flex justify-start">
                        <div className="max-w-[90%] p-2 lg:p-3 rounded-xl lg:rounded-2xl border bg-slate-800/50 border-slate-600">
                          <div className="flex items-center gap-2 mb-1">
                            <Loader2 className="w-3 lg:w-4 h-3 lg:h-4 text-amber-500 animate-spin" />
                            <span className="text-xs text-amber-400">Weryfikacja w toku...</span>
                          </div>
                          <p className="text-xs lg:text-sm text-slate-300 whitespace-pre-wrap">{verification.streamingContent}</p>
                        </div>
                      </div>
                    )}

                    {!verification.isStreaming && verification.results.map((result, index) => (
                      <div key={index} className="flex justify-start">
                        <div className={`max-w-[90%] p-2 lg:p-3 rounded-xl lg:rounded-2xl border ${getVerificationColor(result.verification)}`}>
                          <div className="flex items-center gap-2 mb-1">
                            {getVerificationIcon(result.verification)}
                            <span className={`font-medium text-xs lg:text-sm ${
                              result.verification === 'VERIFIED' ? 'text-green-400' :
                              result.verification === 'FALSE' ? 'text-red-400' :
                              'text-yellow-400'
                            }`}>
                              {getVerificationLabel(result.verification)}
                            </span>
                            <span className="text-slate-400 text-xs">
                              ({Math.round(result.confidence * 100)}%)
                            </span>
                          </div>
                          <p className="text-xs lg:text-sm text-slate-200">{result.explanation}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ))
              )}
              
              {isVerifying && verifications.every(v => v.isStreaming === false) && (
                <div className="flex justify-start">
                  <div className="bg-slate-800 p-2 lg:p-3 rounded-xl lg:rounded-2xl">
                    <Loader2 className="w-4 lg:w-5 h-4 lg:h-5 text-amber-500 animate-spin" />
                  </div>
                </div>
              )}
              <div ref={verificationEndRef} />
            </div>

            <div className="p-3 lg:p-4 border-t border-slate-700">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={verificationInput}
                  onChange={(e) => setVerificationInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleVerify()}
                  placeholder="Wpisz stwierdzenie do weryfikacji..."
                  disabled={isVerifying}
                  className="flex-1 px-3 lg:px-4 py-2 lg:py-3 bg-slate-800 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500 disabled:opacity-50"
                />
                <button
                  onClick={handleVerify}
                  disabled={isVerifying || !verificationInput.trim()}
                  className="px-3 lg:px-4 py-2 lg:py-3 bg-amber-500 hover:bg-amber-600 disabled:bg-slate-700 disabled:cursor-not-allowed text-white rounded-xl transition-colors"
                >
                  <Send className="w-4 lg:w-5 h-4 lg:h-5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
