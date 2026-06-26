import React, { useState, useEffect, useRef } from 'react';
import { chatWithAi, createInitialBookingState } from '../services/aiService';
import { useLanguage } from '../context/LanguageContext';

export default function AiFloatingBubble({ setActivePage, activePage }) {
  const { language } = useLanguage();
  const [isLoggedIn, setIsLoggedIn] = useState(() => {
    try {
      return !!localStorage.getItem('accessToken');
    } catch (e) {
      return false;
    }
  });

  const [isOpen, setIsOpen] = useState(false);
  const [bookingState, setBookingState] = useState(() => {
    try {
      const saved = localStorage.getItem('elysianBookingState');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed && typeof parsed === 'object') {
          return parsed;
        }
      }
    } catch (e) {
      console.error("Error parsing bookingState in AiFloatingBubble:", e);
    }
    return createInitialBookingState();
  });
  const [chatMessages, setChatMessages] = useState(() => {
    try {
      const saved = localStorage.getItem('elysianChatMessages');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          return parsed;
        }
      }
    } catch (e) {
      console.error("Error parsing chatMessages in AiFloatingBubble:", e);
    }
    return [];
  });
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const messagesEndRef = useRef(null);

  useEffect(() => {
    try {
      setIsLoggedIn(!!localStorage.getItem('accessToken'));
    } catch (e) {
      setIsLoggedIn(false);
    }
  }, [activePage]);

  // Synchronize access token from localStorage
  useEffect(() => {
    if (isOpen) {
      try {
        const token = localStorage.getItem('accessToken') || '';
        setBookingState(prev => ({
          ...prev,
          access_token: token
        }));
      } catch (e) {
        console.error(e);
      }
    }
  }, [isOpen]);

  // Set up welcome message
  useEffect(() => {
    const welcomeMsgs = {
      VN: 'Xin chào! Tôi có thể hỗ trợ gì cho bạn về hành trình lưu trú hoặc đặt phòng hôm nay?',
      EN: 'Hello! How can I assist you with your travel plans or room bookings today?',
      JP: 'こんにちは！本日のご宿泊やご予約について、何かお手伝いできることはありますか？',
      KR: '안녕하세요! 오늘 여행 계획이나 객실 예약과 관련하여 무엇을 도와드릴까요?',
      CN: '您好！今天有什么我可以帮您的吗？比如查找房间或了解入住流程？'
    };

    const initialGreeting = welcomeMsgs[language] || welcomeMsgs.EN;
    try {
      const saved = localStorage.getItem('elysianChatMessages');
      const parsed = saved ? JSON.parse(saved) : [];
      if (parsed.length === 0) {
        setChatMessages([
          {
            role: 'assistant',
            content: initialGreeting
          }
        ]);
      }
    } catch (e) {
      console.error(e);
    }
  }, [language]);

  // Sync state changes to localStorage and dispatch event
  useEffect(() => {
    if (bookingState) {
      try {
        const newValue = JSON.stringify(bookingState);
        const currentSaved = localStorage.getItem('elysianBookingState');
        if (newValue !== currentSaved) {
          localStorage.setItem('elysianBookingState', newValue);
          window.dispatchEvent(new CustomEvent('elysianSync', { detail: { type: 'bookingState', data: bookingState } }));
        }
      } catch (e) {
        console.error("Error saving bookingState:", e);
      }
    }
  }, [bookingState]);

  useEffect(() => {
    if (chatMessages) {
      try {
        const newValue = JSON.stringify(chatMessages);
        const currentSaved = localStorage.getItem('elysianChatMessages');
        if (newValue !== currentSaved) {
          localStorage.setItem('elysianChatMessages', newValue);
          window.dispatchEvent(new CustomEvent('elysianSync', { detail: { type: 'chatMessages', data: chatMessages } }));
        }
      } catch (e) {
        console.error("Error saving chatMessages:", e);
      }
    }
  }, [chatMessages]);

  // Listen to cross-component and cross-tab sync events
  useEffect(() => {
    const handleSync = (e) => {
      const { type, data } = e.detail;
      if (type === 'bookingState') setBookingState(data);
      if (type === 'chatMessages') setChatMessages(data);
    };
    
    const handleStorage = (e) => {
      if (e.key === 'elysianBookingState' && e.newValue) {
        setBookingState(JSON.parse(e.newValue));
      } else if (e.key === 'elysianChatMessages' && e.newValue) {
        setChatMessages(JSON.parse(e.newValue));
      }
    };

    window.addEventListener('elysianSync', handleSync);
    window.addEventListener('storage', handleStorage);
    return () => {
      window.removeEventListener('elysianSync', handleSync);
      window.removeEventListener('storage', handleStorage);
    };
  }, []);

  // Auto-scroll to bottom of widget chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, isLoading]);

  // Hide bubble if not logged in, or on the dedicated AI Assistant page
  const isDedicatedPage = activePage === 'ai-assistant';

  if (!isLoggedIn || isDedicatedPage) {
    return null;
  }

  // Format message text with basic markdown
  const formatMessageText = (content, onSendMessage) => {
    if (!content) return '';
    
    // Bold parser **text** -> <strong>text</strong>
    let formatted = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // Image parser ![alt](url) -> <img>
    formatted = formatted.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" class="w-full h-auto rounded-xl my-2 shadow-sm object-cover max-h-48" />');
    
    const lines = formatted.split('\n');
    return lines.map((line, idx) => {
      const trimmed = line.trim();
      
      // Parse Custom Generative UI Room Card: [ROOM_CARD: name | price | capacity | url]
      if (trimmed.startsWith('[ROOM_CARD:')) {
        const match = trimmed.match(/\[ROOM_CARD:\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\]/);
        if (match) {
          const [_, name, price, capacity, image] = match;
          return (
            <div key={idx} className="flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden my-2 shadow-sm max-w-[260px]">
              {image && image !== 'null' && image !== '' && (
                <img src={image} alt={name} className="w-full h-24 object-cover" />
              )}
              <div className="p-3 flex flex-col gap-1.5">
                <h4 className="font-bold text-[13px] text-slate-800 m-0">{name}</h4>
                <div className="flex justify-between items-center text-[10px] font-medium text-slate-600">
                  <span>{capacity}</span>
                  <span className="text-primary font-bold">{price}</span>
                </div>
                <button 
                  onClick={() => onSendMessage && onSendMessage(`Tôi chọn phòng ${name}`)}
                  className="mt-1.5 w-full py-2 bg-slate-50 hover:bg-primary hover:text-white border border-slate-200 hover:border-primary rounded-md text-[11px] font-bold transition-colors cursor-pointer"
                >
                  Chọn
                </button>
              </div>
            </div>
          );
        }
      }

      // Parse Custom Generative UI Actions (Quick Replies): [ACTIONS: Option 1 | Option 2]
      if (trimmed.startsWith('[ACTIONS:')) {
        const match = trimmed.match(/\[ACTIONS:\s*(.*?)\]/);
        if (match) {
          const actions = match[1].split('|').map(a => a.trim());
          return (
            <div key={idx} className="flex flex-wrap gap-1.5 my-2">
              {actions.map((action, i) => (
                <button 
                  key={i}
                  onClick={() => onSendMessage && onSendMessage(action)}
                  className="px-2.5 py-1.5 bg-white hover:bg-primary hover:text-white border border-slate-200 hover:border-primary rounded-full text-[10px] font-bold text-slate-700 transition-colors cursor-pointer shadow-sm"
                >
                  {action}
                </button>
              ))}
            </div>
          );
        }
      }

      if (trimmed.startsWith('- ')) {
        return (
          <li key={idx} className="ml-4 list-disc my-1 text-[11px] tracking-wide text-neutral-800 font-medium">
            <span dangerouslySetInnerHTML={{ __html: trimmed.substring(2) }} />
          </li>
        );
      }
      if (trimmed.match(/^\d+\.\s/)) {
        const match = trimmed.match(/^(\d+)\.\s(.*)/);
        return (
          <li key={idx} className="ml-4 list-decimal my-1 text-[11px] tracking-wide text-neutral-800 font-medium">
            <span dangerouslySetInnerHTML={{ __html: match[2] }} />
          </li>
        );
      }
      if (trimmed === '') {
        return <div key={idx} className="h-1.5" />;
      }
      return (
        <p key={idx} className="my-1 leading-relaxed text-[11px] tracking-wide font-medium text-neutral-850 m-0" dangerouslySetInnerHTML={{ __html: trimmed }} />
      );
    });
  };

  const handleSendMessage = async (e) => {
    e?.preventDefault();
    if (!inputText.trim() || isLoading) return;

    const message = inputText;
    setInputText('');
    setIsLoading(true);

    // Append user input
    setChatMessages(prev => [...prev, { role: 'user', content: message }]);

    try {
      const data = await chatWithAi(message, bookingState);
      setBookingState(data.state);
      setChatMessages(prev => [...prev, { role: 'assistant', content: data.response }]);
    } catch (err) {
      console.error(err);
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: language === 'VN' 
          ? '⚠️ Lỗi kết nối với Trợ lý AI.'
          : '⚠️ AI connection error.'
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleExpandUI = () => {
    setIsOpen(false);
    setActivePage('ai-assistant');
  };

  return (
    <div className="fixed bottom-6 right-6 z-[9999] font-['Montserrat'] text-slate-800 text-left select-none">
      
      {/* Floating Chat Window */}
      {isOpen && (
        <div className="absolute bottom-16 right-0 w-[330px] sm:w-[360px] h-[480px] bg-white shadow-[0_8px_30px_rgb(0,0,0,0.12)] rounded-2xl overflow-hidden flex flex-col justify-between animate-scale-in">
          
          {/* Header Panel */}
          <div className="px-4 py-3 bg-neutral-900 text-white flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-full bg-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-[14px]">support_agent</span>
              </div>
              <div className="text-left">
                <h5 className="text-[11.5px] font-black uppercase tracking-wider m-0">Elysian AI Concierge</h5>
                <span className="text-[8.5px] text-emerald-400 font-extrabold uppercase tracking-widest">Online</span>
              </div>
            </div>
            
            <div className="flex items-center gap-1.5">
              <button 
                onClick={handleExpandUI}
                title="Mở rộng giao diện"
                className="p-1 hover:bg-white/10 text-white transition-colors border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-base">open_in_full</span>
              </button>
              <button 
                onClick={() => setIsOpen(false)}
                className="p-1 hover:bg-white/10 text-white transition-colors border-none bg-transparent cursor-pointer flex items-center"
              >
                <span className="material-symbols-outlined text-base">close</span>
              </button>
            </div>
          </div>

          {/* Messages Flow */}
          <div className="flex-grow p-4 overflow-y-auto space-y-3 bg-slate-50/50">
            {chatMessages.map((msg, index) => {
              const isBot = msg.role === 'assistant';
              return (
                <div key={index} className={`flex ${isBot ? 'justify-start' : 'justify-end'}`}>
                  <div className={`flex gap-2 max-w-[85%] ${!isBot ? 'flex-row-reverse' : ''}`}>
                    <div className={`w-6 h-6 rounded-full shrink-0 flex items-center justify-center text-white text-[10px] ${
                      isBot ? 'bg-primary' : 'bg-neutral-800'
                    }`}>
                      <span className="material-symbols-outlined text-xs">
                        {isBot ? 'support_agent' : 'person'}
                      </span>
                    </div>
                    <div className={`p-3 border text-xs leading-relaxed font-medium ${
                      isBot 
                        ? 'bg-white text-slate-800 border-slate-100 rounded-2xl rounded-tl-sm shadow-sm' 
                        : 'bg-neutral-900 text-white border-neutral-900 rounded-2xl rounded-tr-sm shadow-md'
                    }`}>
                      {isBot ? (
                        <div className="space-y-1 text-left text-[11px] font-medium leading-relaxed">
                          {formatMessageText(msg.content, handleSendMessage)}
                        </div>
                      ) : (
                        <p className="m-0 whitespace-pre-wrap">{msg.content}</p>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
            
            {/* Loading Indicator */}
            {isLoading && (
              <div className="flex justify-start">
                <div className="flex gap-2 items-center">
                  <div className="w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center text-[10px]">
                    <span className="material-symbols-outlined text-xs animate-spin">sync</span>
                  </div>
                  <div className="p-3 bg-white border border-slate-100 rounded-2xl rounded-tl-sm shadow-sm flex gap-0.5 items-center">
                    <span className="w-1 h-1 rounded-full bg-slate-400 animate-bounce" />
                    <span className="w-1 h-1 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-1 h-1 rounded-full bg-slate-400 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Booking Flow Redirect Tip */}
          {bookingState.step !== 'idle' && (
            <div className="px-4 py-1.5 bg-red-50 border-t border-red-150 text-[10px] font-bold text-primary text-center">
              ⚠️ Đang ở bước đặt phòng. <button onClick={handleExpandUI} className="underline bg-transparent border-none text-primary font-black uppercase cursor-pointer">Nhấn vào đây để xem chi tiết</button>
            </div>
          )}

          {/* Quick suggestions chips in floating popup */}
          {bookingState.step === 'idle' && (
            <div className="px-4 py-1.5 bg-white border-t border-slate-100 flex gap-1.5 overflow-x-auto scrollbar-none whitespace-nowrap">
              <button 
                onClick={() => setInputText(language === 'VN' ? 'Khách sạn có những loại phòng nào?' : 'What room types are available?')}
                className="px-3 py-1 rounded-full bg-slate-50 hover:bg-slate-100 text-[10px] font-bold tracking-wide text-slate-600 hover:text-primary border border-slate-200 hover:border-primary/30 transition-all cursor-pointer"
              >
                Các loại phòng?
              </button>
              <button 
                onClick={() => setInputText(language === 'VN' ? 'Quy trình check-in tự động' : 'Smart Check-in process')}
                className="px-3 py-1 rounded-full bg-slate-50 hover:bg-slate-100 text-[10px] font-bold tracking-wide text-slate-600 hover:text-primary border border-slate-200 hover:border-primary/30 transition-all cursor-pointer"
              >
                Check-in FaceID
              </button>
            </div>
          )}

          {/* Input Box */}
          <form onSubmit={handleSendMessage} className="p-3 border-t border-slate-100 flex gap-2 bg-white items-center">
            <input 
              type="text" 
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              disabled={isLoading}
              placeholder={language === 'VN' ? 'Nhập câu hỏi tại đây...' : 'Ask something...'}
              className="flex-grow px-4 py-2 bg-slate-50 border border-slate-200 rounded-full text-xs font-semibold text-slate-800 focus:outline-none focus:border-primary focus:bg-white transition-colors"
            />
            <button 
              type="submit"
              disabled={isLoading || !inputText.trim()}
              className="w-9 h-9 shrink-0 rounded-full bg-primary disabled:bg-slate-300 text-white flex items-center justify-center border-none cursor-pointer hover:bg-slate-900 transition-colors shadow-sm"
            >
              <span className="material-symbols-outlined text-[16px]">send</span>
            </button>
          </form>
        </div>
      )}

      {/* Circle Floating Bubble Trigger Button */}
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="w-14 h-14 rounded-full bg-primary hover:bg-red-700 text-white shadow-2xl flex items-center justify-center border-none cursor-pointer transition-all duration-300 hover:scale-110 active:scale-95 group relative"
      >
        <span className="material-symbols-outlined text-2xl group-hover:rotate-12 transition-transform duration-300">
          {isOpen ? 'close' : 'chat_bubble'}
        </span>
        
        {/* Soft pulse effect */}
        {!isOpen && (
          <span className="absolute inset-0 rounded-full bg-primary/30 animate-ping -z-10" />
        )}
      </button>

    </div>
  );
}
