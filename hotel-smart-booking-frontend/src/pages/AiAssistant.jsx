import React, { useState, useEffect, useRef } from 'react';
import { 
  chatWithAi, 
  startBookingWithAi, 
  cancelBookingWithAi, 
  createInitialBookingState 
} from '../services/aiService';
import { useLanguage } from '../context/LanguageContext';

// Welcome messages for AI Assistant
const welcomeMsgs = {
  VN: 'Xin chào! Tôi là Trợ lý Ảo Elysian Cần Thơ. Tôi có thể giúp bạn tìm kiếm phòng và đồng hành cùng bạn đặt phòng thông minh 24/7. Bạn muốn đặt phòng khi nào?',
  EN: 'Hello! I am your Elysian Can Tho AI Concierge. I can help you search rooms and guide you through our smart booking flow 24/7. When would you like to book?',
  JP: 'こんにちは！エリシアンカントーAIコンシェルジュです。ホテルの客室検索、スマートな予約手続きを24時間年中無休でお手伝いいたします。いつ予約されますか？',
  KR: '안녕하세요! 엘리시안 끈터 AI 컨시어지입니다. 객실 검색, 스마트한 예약 절차를 24시간 언제든 도와드릴 수 있습니다. 언제 예약하시겠습니까?',
  CN: '您好！我是极乐芹苴AI智能助理。我可以帮您查找客房，并引导您完成24/7自助预订。您想什么时候预订客房？'
};

// Suggested prompt chips
const suggestionChips = {
  VN: [
    'Các loại phòng tại Elysian Cần Thơ',
    'Hướng dẫn check-in bằng khuôn mặt',
    'Ưu đãi thành viên Elysian Rewards'
  ],
  EN: [
    'What room types are available?',
    'How does FaceID check-in work?',
    'Elysian Rewards member benefits'
  ],
  JP: [
    '客室タイプの一覧',
    '顔認証チェックイン方法',
    'エリシアンリワーズの会員特典'
  ],
  KR: [
    '객실 유형 목록',
    '안면인식 체크인 안내',
    '엘리시안 리워즈 회원 혜택'
  ],
  CN: [
    '房型类别列表',
    '人脸识别入住指南',
    '极乐会员权益'
  ]
};

// Helper to fetch matching Unsplash image based on location keywords (Cần Thơ focus)
const getHotelPlaceholderImage = (hotel) => {
  const locationLower = (hotel.location || '').toLowerCase();
  if (locationLower.includes('cần thơ') || locationLower.includes('can tho')) {
    return 'https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=600&q=80'; // Premium resort / hotel image
  }
  return 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=600&q=80';
};

export default function AiAssistant({ setActivePage }) {
  const { language, t } = useLanguage();
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
      console.error("Error parsing bookingState from localStorage:", e);
    }
    return createInitialBookingState();
  });
  const [isLoggedIn, setIsLoggedIn] = useState(() => {
    try {
      return !!localStorage.getItem('accessToken');
    } catch (e) {
      return false;
    }
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
      console.error("Error parsing chatMessages from localStorage:", e);
    }
    return [];
  });
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const messagesEndRef = useRef(null);

  const getLanguageChips = () => {
    return suggestionChips[language] || suggestionChips.EN;
  };

  // Set up welcome message
  useEffect(() => {
    const initialGreeting = welcomeMsgs[language] || welcomeMsgs.EN;
    const saved = localStorage.getItem('elysianChatMessages');
    const parsed = saved ? JSON.parse(saved) : [];
    if (parsed.length === 0) {
      setChatMessages([
        {
          role: 'assistant',
          content: initialGreeting,
          searchResults: null
        }
      ]);
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

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, isLoading]);

  // Sync token when component loads or updates
  useEffect(() => {
    const token = localStorage.getItem('accessToken') || '';
    setIsLoggedIn(!!token);
    setBookingState(prev => ({
      ...prev,
      access_token: token
    }));
  }, []);

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
            <div key={idx} className="flex flex-col bg-white border border-slate-200 rounded-xl overflow-hidden my-3 shadow-sm max-w-sm">
              {image && image !== 'null' && image !== '' && (
                <img src={image} alt={name} className="w-full h-40 object-cover" />
              )}
              <div className="p-4 flex flex-col gap-2">
                <h4 className="font-bold text-[15px] text-slate-800 m-0">{name}</h4>
                <div className="flex justify-between items-center text-[13px] font-medium text-slate-600">
                  <span>Sức chứa: {capacity}</span>
                  <span className="text-primary font-bold">{price}</span>
                </div>
                <button 
                  onClick={() => onSendMessage && onSendMessage(`Tôi chọn phòng ${name}`)}
                  className="mt-2 w-full py-2.5 bg-slate-50 hover:bg-primary hover:text-white border border-slate-200 hover:border-primary rounded-lg text-[13px] font-bold transition-colors cursor-pointer"
                >
                  Chọn phòng này
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
            <div key={idx} className="flex flex-wrap gap-2 my-2.5">
              {actions.map((action, i) => (
                <button 
                  key={i}
                  onClick={() => onSendMessage && onSendMessage(action)}
                  className="px-3.5 py-1.5 bg-white hover:bg-primary hover:text-white border border-slate-200 hover:border-primary rounded-full text-[12px] font-bold text-slate-700 transition-colors cursor-pointer shadow-sm"
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
          <li key={idx} className="ml-5 list-disc my-1 text-[13px] tracking-wide text-neutral-800 font-medium">
            <span dangerouslySetInnerHTML={{ __html: trimmed.substring(2) }} />
          </li>
        );
      }
      if (trimmed.match(/^\d+\.\s/)) {
        const match = trimmed.match(/^(\d+)\.\s(.*)/);
        return (
          <li key={idx} className="ml-5 list-decimal my-1 text-[13px] tracking-wide text-neutral-800 font-medium">
            <span dangerouslySetInnerHTML={{ __html: match[2] }} />
          </li>
        );
      }
      if (trimmed === '') {
        return <div key={idx} className="h-2" />;
      }
      return (
        <p key={idx} className="my-1 leading-relaxed text-[13px] tracking-wide font-medium text-neutral-850" dangerouslySetInnerHTML={{ __html: trimmed }} />
      );
    });
  };



  // Price formatter
  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  // Handle message send
  const handleSendMessage = async (textToSend) => {
    const message = textToSend || inputText;
    if (!message.trim() || isLoading) return;

    setInputText('');
    setIsLoading(true);

    // Append user message immediately
    setChatMessages(prev => [...prev, { role: 'user', content: message }]);

    try {
      const data = await chatWithAi(message, bookingState);
      
      // Update state returned from AI
      setBookingState(data.state);

      // Append AI response
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: data.response,
        searchResults: data.search_results
      }]);
    } catch (err) {
      console.error(err);
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: language === 'VN' 
          ? '⚠️ Hệ thống Trợ lý ảo hiện đang bận hoặc đang bảo trì. Vui lòng thử lại sau ít phút!'
          : '⚠️ The AI Assistant is temporarily busy or undergoing maintenance. Please try again in a few moments.',
        searchResults: null
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  // Directly start booking from a hotel search card
  const handleSelectHotel = async (hotel) => {
    setIsLoading(true);
    // Append system visual log to chat
    setChatMessages(prev => [...prev, { 
      role: 'user', 
      content: language === 'VN' 
        ? `Đặt phòng tại khách sạn: ${hotel.name}` 
        : `Book room at: ${hotel.name}` 
    }]);

    try {
      const data = await startBookingWithAi(hotel, bookingState);
      setBookingState(data.state);
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: data.response,
        searchResults: null
      }]);
    } catch (err) {
      console.error(err);
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: '⚠️ Không thể khởi động quy trình đặt phòng.',
        searchResults: null
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  // Cancel/Abort current booking flow
  const handleCancelBooking = async () => {
    setIsLoading(true);
    try {
      const data = await cancelBookingWithAi(bookingState);
      setBookingState(data.state);
      setChatMessages(prev => [...prev, {
        role: 'assistant',
        content: data.response,
        searchResults: null
      }]);
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  // Clear chat history
  const handleClearChat = () => {
    try {
      localStorage.removeItem('elysianChatMessages');
      localStorage.removeItem('elysianBookingState');
    } catch (e) {
      console.error("Error clearing chat from localStorage:", e);
    }
    const initialGreeting = welcomeMsgs[language] || welcomeMsgs.EN;
    setChatMessages([
      {
        role: 'assistant',
        content: initialGreeting,
        searchResults: null
      }
    ]);
    setBookingState(createInitialBookingState());
  };

  const renderQuickActions = () => {
    if (isLoading) return null;
    const currentStep = bookingState.step;

    if (currentStep === 'date_guests') {
      return (
        <div className="flex flex-wrap gap-2 mt-3 animate-fade-in">
          <button 
            onClick={() => handleSendMessage('Nhận phòng từ 2026-07-10 đến 2026-07-15')}
            className="px-3 py-1.5 border border-slate-200 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 transition-colors uppercase tracking-wider cursor-pointer"
          >
            📅 Đi ngày 10/7 - 15/7
          </button>
          <button 
            onClick={() => handleSendMessage('Đặt cho 2 người lớn')}
            className="px-3 py-1.5 border border-slate-200 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 transition-colors uppercase tracking-wider cursor-pointer"
          >
            👥 2 Người lớn
          </button>
        </div>
      );
    }

    if (currentStep === 'checkin_method') {
      return (
        <div className="flex flex-wrap gap-2 mt-3 animate-fade-in">
          <button 
            onClick={() => handleSendMessage('Check-in FaceID')}
            className="px-3 py-1.5 border border-primary text-xs font-bold text-primary bg-white hover:bg-red-50 transition-colors uppercase tracking-wider cursor-pointer flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[14px]">face</span> FaceID / Khuôn mặt
          </button>
          <button 
            onClick={() => handleSendMessage('Check-in Mã QR')}
            className="px-3 py-1.5 border border-primary text-xs font-bold text-primary bg-white hover:bg-red-50 transition-colors uppercase tracking-wider cursor-pointer flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[14px]">qr_code</span> Mã QR tự động
          </button>
          <button 
            onClick={() => handleSendMessage('Check-in tại quầy lễ tân')}
            className="px-3 py-1.5 border border-slate-350 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 transition-colors uppercase tracking-wider cursor-pointer flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[14px]">room_service</span> Quầy lễ tân
          </button>
        </div>
      );
    }

    if (currentStep === 'confirm') {
      return (
        <div className="flex flex-wrap gap-2 mt-3 animate-fade-in">
          <button 
            onClick={() => handleSendMessage('Đồng ý điều khoản và xác nhận đặt phòng')}
            className="px-4 py-2 bg-primary text-white text-xs font-bold hover:brightness-110 transition-all uppercase tracking-wider cursor-pointer"
          >
            Đồng ý & Đặt ngay
          </button>
        </div>
      );
    }

    return null;
  };

  const getLockMessages = () => {
    const messages = {
      VN: {
        title: 'YÊU CẦU ĐĂNG NHẬP',
        desc: 'Trợ lý ảo Elysian AI là tính năng độc quyền dành riêng cho Hội viên đã đăng nhập. Vui lòng đăng nhập để trải nghiệm đặt phòng thông minh.',
        loginBtn: 'Đăng nhập ngay',
        homeBtn: 'Về trang chủ'
      },
      EN: {
        title: 'LOGIN REQUIRED',
        desc: 'Elysian AI Assistant is an exclusive feature for registered members. Please login to experience smart room booking.',
        loginBtn: 'Login Now',
        homeBtn: 'Back to Home'
      },
      JP: {
        title: 'ログインが必要です',
        desc: 'エリシアン AI アシスタントは登録メンバー専用の機能です。スマートな予約手続きを体験するにはログインしてください。',
        loginBtn: '今すぐログイン',
        homeBtn: 'ホームに戻る'
      },
      KR: {
        title: '로그인 필요',
        desc: '엘리시안 AI 컨시어지는 등록된 회원 전용 기능입니다. 스마트한 객실 예약을 위해 로그인해 주세요.',
        loginBtn: '지금 로그인',
        homeBtn: '홈으로 돌아가기'
      },
      CN: {
        title: '需要登录',
        desc: '极乐AI助手是注册会员专属功能。请登录以体验智能客房预订。',
        loginBtn: '立即登录',
        homeBtn: '返回首页'
      }
    };
    return messages[language] || messages.EN;
  };

  const lockMsgs = getLockMessages();

  if (!isLoggedIn) {
    return (
      <div className="pt-24 min-h-screen bg-slate-50 flex flex-col font-['Montserrat']">
        {/* Header Banner */}
        <div className="bg-neutral-900 py-10 px-4 md:px-margin-desktop text-left text-white border-b border-primary relative overflow-hidden">
          <div className="absolute right-0 top-0 bottom-0 w-1/3 elysian-pattern opacity-15 hidden md:block" />
          <div className="max-w-7xl mx-auto relative z-10">
            <span className="text-[10px] font-black tracking-[0.25em] text-primary block mb-2 uppercase">
              Elysian Smart Hotel Concierge
            </span>
            <h1 className="text-3xl font-black tracking-wider uppercase mb-2">
              {t('ai_title', 'TRỢ LÝ ẢO ELYSIAN')}
            </h1>
            <p className="text-xs text-neutral-400 font-bold uppercase tracking-wider">
              {t('ai_subtitle', 'Tư vấn hành trình nghỉ dưỡng & Đặt phòng thông minh 24/7')}
            </p>
          </div>
        </div>

        {/* Lock Screen Centered Container */}
        <div className="flex-grow flex items-center justify-center py-16 px-4">
          <div className="max-w-md w-full bg-white border border-slate-200 p-8 md:p-10 shadow-xl text-center flex flex-col items-center">
            <div className="w-16 h-16 rounded-full bg-amber-50 flex items-center justify-center text-amber-500 mb-6 border border-amber-250 animate-pulse">
              <span className="material-symbols-outlined text-3xl font-semibold">lock</span>
            </div>
            
            <h2 className="text-lg font-black text-slate-900 uppercase tracking-widest mb-3">
              {lockMsgs.title}
            </h2>
            
            <p className="text-xs text-slate-500 font-bold uppercase tracking-wider mb-6 leading-relaxed">
              {lockMsgs.desc}
            </p>
            
            <div className="w-full flex flex-col gap-3">
              <button
                onClick={() => setActivePage('login')}
                className="w-full bg-primary hover:bg-slate-950 text-white font-bold py-3.5 px-8 uppercase text-[11px] tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-2"
              >
                <span className="material-symbols-outlined text-base">login</span> {lockMsgs.loginBtn}
              </button>
              
              <button
                onClick={() => setActivePage('home')}
                className="w-full bg-white hover:bg-slate-50 text-slate-700 font-bold py-3 px-8 uppercase text-[10px] tracking-widest transition-all cursor-pointer border border-slate-250 flex items-center justify-center gap-1.5"
              >
                <span className="material-symbols-outlined text-sm">home</span> {lockMsgs.homeBtn}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-24 min-h-screen bg-slate-50 flex flex-col font-['Montserrat'] overflow-hidden">
      
      {/* --- ACTIVE STATE (SPLIT VIEW) --- */}
      <div className="w-full flex-grow flex flex-col transition-all duration-700 ease-in-out opacity-100 animate-fade-in">
        
        {/* Subtle Header Banner */}
        <div className="bg-white border-b border-slate-100 py-4 px-6 flex items-center justify-between shadow-[0_4px_20px_rgb(0,0,0,0.02)]">
          <div className="flex items-center gap-3">
             <div className="w-9 h-9 rounded-full bg-primary shadow-md flex items-center justify-center">
               <span className="material-symbols-outlined text-white text-[18px] font-light">support_agent</span>
             </div>
             <div>
               <h1 className="text-[13px] font-bold text-slate-900 tracking-wide">Elysian AI Concierge</h1>
               <p className="text-[10px] font-bold text-primary tracking-widest uppercase mt-0.5 flex items-center gap-1">
                 <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span> Active Session
               </p>
             </div>
          </div>
          <button 
            onClick={handleClearChat}
            className="px-4 py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-xl text-[11px] font-bold tracking-wide transition-colors flex items-center gap-1.5 border border-slate-200 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span> Restart
          </button>
        </div>

        <div className="max-w-7xl mx-auto w-full px-4 md:px-6 py-8 flex-grow grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: Chat Box */}
          <div className="lg:col-span-8 flex flex-col h-[650px] bg-white rounded-3xl border border-slate-200 shadow-sm relative overflow-hidden">
            
            {/* Chat Messages Log */}
            <div className="flex-grow overflow-y-auto p-6 space-y-6 bg-slate-50/50">
              {chatMessages.map((msg, index) => {
                const isBot = msg.role === 'assistant';
                
                return (
                  <div key={index} className={`flex flex-col ${isBot ? 'items-start' : 'items-end'} animate-fade-in-up`}>
                    <div className={`flex gap-3 max-w-[85%] ${!isBot ? 'flex-row-reverse' : ''}`}>
                      {/* Avatar */}
                      <div className={`w-9 h-9 rounded-full shrink-0 flex items-center justify-center shadow-sm ${
                        isBot ? 'bg-primary text-white' : 'bg-white border border-slate-200 text-slate-700'
                      }`}>
                        <span className="material-symbols-outlined text-[18px] font-light">
                          {isBot ? 'support_agent' : 'person'}
                        </span>
                      </div>

                      {/* Chat Bubble */}
                      <div className={`p-4 rounded-2xl ${
                        isBot 
                          ? 'bg-white text-slate-800 border border-slate-200 shadow-sm rounded-tl-sm' 
                          : 'bg-neutral-900 text-white shadow-md rounded-tr-sm'
                      }`}>
                        {isBot ? (
                          <div className="space-y-1.5 text-left text-[14px] font-medium leading-relaxed">
                            {formatMessageText(msg.content, handleSendMessage)}
                          </div>
                        ) : (
                          <p className="text-[14px] tracking-wide font-medium text-left m-0">{msg.content}</p>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}

              {/* AI Typing Loading Indicator */}
              {isLoading && (
                <div className="flex items-start gap-3 animate-fade-in-up">
                  <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-white shadow-sm">
                    <span className="material-symbols-outlined text-[18px] font-light animate-spin">sync</span>
                  </div>
                  <div className="p-4 bg-white border border-slate-200 shadow-sm rounded-2xl rounded-tl-sm flex items-center gap-1.5 h-[52px]">
                    <span className="w-2 h-2 rounded-full bg-primary/60 animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="w-2 h-2 rounded-full bg-primary/60 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-2 h-2 rounded-full bg-primary/60 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Quick Actions Suggestions */}
            <div className="px-5 py-3 bg-white border-t border-slate-100 text-left">
              {bookingState.step !== 'idle' ? renderQuickActions() : (
                <div className="flex flex-wrap gap-2">
                  {getLanguageChips().map((chip, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSendMessage(chip)}
                      className="px-3 py-1.5 bg-slate-50 hover:bg-red-50 text-[11px] font-bold tracking-wider text-slate-600 hover:text-primary border border-slate-200 hover:border-primary/50 transition-colors cursor-pointer rounded-lg uppercase"
                    >
                      {chip}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Chat Form Input (Active State) */}
            <div className="p-4 bg-white border-t border-slate-100">
              <form 
                onSubmit={(e) => { e.preventDefault(); handleSendMessage(); }}
                className="w-full bg-slate-50 rounded-2xl border border-slate-200 p-1.5 flex items-end gap-2 focus-within:bg-white focus-within:border-primary/40 focus-within:shadow-[0_0_20px_rgba(162,5,19,0.1)] transition-all"
              >
                <textarea 
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage();
                    }
                  }}
                  disabled={isLoading}
                  placeholder={language === 'VN' ? 'Nhập phản hồi của bạn...' : 'Type your reply...'}
                  className="flex-grow p-3 px-4 text-[14px] text-slate-800 bg-transparent resize-none focus:outline-none placeholder:text-slate-400 font-medium max-h-[120px] min-h-[46px]"
                  rows={1}
                />
                <button 
                  type="submit"
                  disabled={isLoading || !inputText.trim()}
                  className="w-10 h-10 mb-1 mr-1 shrink-0 rounded-xl bg-neutral-900 text-white flex items-center justify-center hover:bg-neutral-800 disabled:bg-slate-200 disabled:text-slate-400 transition-colors cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg">arrow_upward</span>
                </button>
              </form>
            </div>
          </div>

          {/* Right Column: Real-time Booking Monitor Panel */}
          <div className="lg:col-span-4 flex flex-col gap-6 w-full text-left">
            
            {/* Modern Timeline / Stepper */}
            <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col gap-5">
              <h4 className="text-[11px] font-black uppercase tracking-[0.2em] text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-4">
                <span className="material-symbols-outlined text-primary text-[18px]">route</span> Tiến trình
              </h4>
              
              <div className="space-y-6 relative pl-5 border-l-2 border-slate-100 ml-2 py-2">
                {[
                  { stepId: 'idle', label: 'Tư vấn hành trình' },
                  { stepId: 'select_room', label: 'Chọn Hạng phòng' },
                  { stepId: 'select_dates', label: 'Ngày & Số khách' },
                  { stepId: 'confirm', label: 'Xác nhận' },
                  { stepId: 'done', label: 'Hoàn tất' }
                ].map((stepObj, idx) => {
                  const isCurrent = bookingState.step === stepObj.stepId;
                  const stepsOrder = ['idle', 'select_room', 'select_dates', 'confirm', 'done'];
                  const currentIdx = stepsOrder.indexOf(bookingState.step);
                  const stepIdx = stepsOrder.indexOf(stepObj.stepId);
                  const isCompleted = stepIdx < currentIdx || bookingState.step === 'done';

                  return (
                    <div key={idx} className="relative text-left">
                      <div className={`absolute -left-[29px] top-1/2 -translate-y-1/2 w-3.5 h-3.5 rounded-full ring-4 ring-white flex items-center justify-center transition-all duration-300 ${
                        isCurrent 
                          ? 'bg-primary scale-125 shadow-[0_0_12px_rgba(162,5,19,0.4)]'
                          : isCompleted
                            ? 'bg-emerald-400'
                            : 'bg-slate-200'
                      }`} />
                      <div className="pl-3">
                        <span className={`text-[13px] tracking-wide block transition-colors ${
                          isCurrent ? 'text-slate-900 font-bold' : isCompleted ? 'text-slate-600 font-semibold' : 'text-slate-400 font-medium'
                        }`}>
                          {stepObj.label}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Modern Booking State Monitor Card */}
            <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col gap-4">
              <h4 className="text-[11px] font-black uppercase tracking-[0.2em] text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-4">
                <span className="material-symbols-outlined text-primary text-[18px]">analytics</span> Tóm tắt yêu cầu
              </h4>

              {bookingState.hotel ? (
                <div className="space-y-4 animate-fade-in text-[13px] font-semibold text-slate-700">
                  <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                    <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Khách sạn</span>
                    <span className="text-[14px] font-black text-slate-900 tracking-wide">{bookingState.hotel.name}</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                      <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Nhận phòng</span>
                      <span className="text-[13px] font-bold text-slate-800">{bookingState.check_in || '--'}</span>
                    </div>
                    <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                      <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Trả phòng</span>
                      <span className="text-[13px] font-bold text-slate-800">{bookingState.check_out || '--'}</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                      <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Hạng phòng</span>
                      <span className="text-[13px] font-bold text-slate-800">{bookingState.room_type_name || '--'}</span>
                    </div>
                    <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                      <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1">Check-in</span>
                      <span className="text-[13px] font-bold text-slate-800">{bookingState.check_in_method || '--'}</span>
                    </div>
                  </div>

                  {bookingState.booking_result && (
                    <div className="bg-red-50 border border-red-200 rounded-2xl p-5 mt-2 shadow-sm">
                      <span className="text-[9px] font-black text-primary uppercase tracking-widest block mb-1">Mã Đặt Phòng</span>
                      <span className="text-lg font-black block tracking-widest font-mono text-slate-900 mb-4">{bookingState.booking_result.bookingCode || bookingState.booking_result.bookingReference || 'ELYSIAN-SUCCESS'}</span>
                      <button 
                        onClick={() => { handleClearChat(); setActivePage('dashboard'); }}
                        className="w-full py-3 bg-primary text-white rounded-xl font-bold text-[11px] uppercase tracking-wider hover:brightness-110 transition-all shadow-md cursor-pointer border-none"
                      >
                        Tới Dashboard xem lịch sử
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="py-10 text-center text-slate-400 flex flex-col items-center">
                  <span className="material-symbols-outlined text-5xl mb-4 text-slate-200">hotel_class</span>
                  <p className="text-[12px] font-semibold leading-relaxed tracking-wide">Chưa có thông tin.<br/>Hãy trò chuyện với Trợ lý để đặt phòng.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
