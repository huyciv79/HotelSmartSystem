import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { chatWithAi, createInitialBookingState } from '../services/aiService';
import { useLanguage } from './LanguageContext';

const AiChatContext = createContext(null);
const MESSAGES_KEY = 'elysianChatMessages';
const STATE_KEY = 'elysianBookingState';
const UI_KEY = 'elysianAiChatUi';

const welcomeMessages = {
  VN: 'Xin chào! Tôi là Trợ lý Ảo The Iris Cần Thơ. Tôi có thể giúp bạn tìm kiếm phòng và đồng hành cùng bạn đặt phòng thông minh 24/7. Bạn muốn đặt phòng khi nào?',
  EN: 'Hello! I am your The Iris Can Tho AI Concierge. How can I help you plan your stay today?',
};

const blankForm = {
  checkIn: '', checkOut: '', adults: 1, children: 0, quantity: 1, checkInMethod: 'Manual',
};

const readJson = (key, fallback) => {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
};

export function AiChatProvider({ children }) {
  const { language } = useLanguage();
  const [bookingState, setBookingState] = useState(() => readJson(STATE_KEY, createInitialBookingState()));
  const [chatMessages, setChatMessages] = useState(() => readJson(MESSAGES_KEY, []));
  const [uiState, setUiState] = useState(() => ({
    isBookingFormOpen: false,
    bookingFormError: '',
    bookingForm: blankForm,
    ...readJson(UI_KEY, {}),
  }));
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const welcome = welcomeMessages[language] || welcomeMessages.EN;
  const addMessage = (message) => setChatMessages((previous) => [...previous, message]);

  useEffect(() => {
    if (chatMessages.length === 0) addMessage({ role: 'assistant', content: welcome, searchResults: null });
  }, [language]); // Only create a greeting for a completely new session; changing language must not erase a booking.

  useEffect(() => {
    localStorage.setItem(STATE_KEY, JSON.stringify(bookingState));
    window.dispatchEvent(new CustomEvent('elysianSync', { detail: { type: 'bookingState', data: bookingState } }));
  }, [bookingState]);

  useEffect(() => {
    localStorage.setItem(MESSAGES_KEY, JSON.stringify(chatMessages));
    window.dispatchEvent(new CustomEvent('elysianSync', { detail: { type: 'chatMessages', data: chatMessages } }));
  }, [chatMessages]);

  useEffect(() => {
    localStorage.setItem(UI_KEY, JSON.stringify(uiState));
    window.dispatchEvent(new CustomEvent('elysianSync', { detail: { type: 'chatUi', data: uiState } }));
  }, [uiState]);

  useEffect(() => {
    const sync = (event) => {
      const { type, data } = event.detail || {};
      if (type === 'bookingState') setBookingState(data);
      if (type === 'chatMessages') setChatMessages(data);
      if (type === 'chatUi') setUiState(data);
    };
    const storage = (event) => {
      if (!event.newValue) return;
      if (event.key === STATE_KEY) setBookingState(JSON.parse(event.newValue));
      if (event.key === MESSAGES_KEY) setChatMessages(JSON.parse(event.newValue));
      if (event.key === UI_KEY) setUiState(JSON.parse(event.newValue));
    };
    window.addEventListener('elysianSync', sync);
    window.addEventListener('storage', storage);
    return () => {
      window.removeEventListener('elysianSync', sync);
      window.removeEventListener('storage', storage);
    };
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('accessToken') || '';
    setBookingState((previous) => previous.access_token === token ? previous : { ...previous, access_token: token });
  }, []);

  const sendMessage = async (textToSend, options = {}) => {
    const { openBookingFormAfterResponse = false, bookingUpdates = null } = options;
    const message = (textToSend || inputText).trim();
    if (!message || isLoading) return;

    setInputText('');
    setIsLoading(true);
    addMessage({ role: 'user', content: message });
    try {
      const stateForRequest = bookingUpdates ? { ...bookingState, ...bookingUpdates } : bookingState;
      const data = await chatWithAi(message, stateForRequest, language);
      setBookingState(data.state);
      if (data.state?.step === 'confirm' && data.state?.error) {
        setUiState((previous) => ({
          ...previous,
          isBookingFormOpen: true,
          bookingFormError: data.state.error,
          bookingForm: {
            ...previous.bookingForm,
            checkIn: data.state.check_in || previous.bookingForm.checkIn,
            checkOut: data.state.check_out || previous.bookingForm.checkOut,
            adults: data.state.adults ?? previous.bookingForm.adults,
            children: data.state.children ?? previous.bookingForm.children,
            quantity: data.state.quantity ?? previous.bookingForm.quantity,
            checkInMethod: data.state.check_in_method || previous.bookingForm.checkInMethod,
          },
        }));
      } else if (openBookingFormAfterResponse) {
        setUiState((previous) => ({ ...previous, isBookingFormOpen: true, bookingFormError: '' }));
      }
      addMessage({ role: 'assistant', content: data.response, searchResults: data.search_results });
    } catch (error) {
      console.error(error);
      const timedOut = error?.code === 'ECONNABORTED' || error?.message?.toLowerCase().includes('timeout');
      addMessage({
        role: 'assistant',
        content: timedOut
          ? (language === 'VN' ? '⌛ Trợ lý AI đang phản hồi chậm hơn bình thường. Vui lòng thử lại sau ít phút.' : '⌛ The AI Assistant is taking longer than usual. Please try again shortly.')
          : (language === 'VN' ? '⚠️ Không thể kết nối đến Trợ lý AI lúc này. Vui lòng thử lại sau ít phút.' : '⚠️ We could not connect to the AI Assistant. Please try again shortly.'),
      });
    } finally {
      setIsLoading(false);
    }
  };

  const selectRoom = (roomName) => sendMessage(`Tôi chọn phòng ${roomName}`, { openBookingFormAfterResponse: true });
  const updateForm = (changes) => setUiState((previous) => ({ ...previous, bookingForm: { ...previous.bookingForm, ...changes }, bookingFormError: '' }));
  const closeForm = () => setUiState((previous) => ({ ...previous, isBookingFormOpen: false, bookingFormError: '' }));

  const submitBookingForm = (event) => {
    event?.preventDefault();
    const form = uiState.bookingForm;
    if (!form.checkIn || !form.checkOut) return setUiState((previous) => ({ ...previous, bookingFormError: 'Vui lòng chọn ngày nhận và ngày trả phòng.' }));
    if (form.checkOut <= form.checkIn) return setUiState((previous) => ({ ...previous, bookingFormError: 'Ngày trả phòng phải sau ngày nhận phòng.' }));
    const bookingUpdates = {
      step: 'idle', check_in: form.checkIn, check_out: form.checkOut, adults: form.adults,
      children: form.children, quantity: form.quantity, check_in_method: form.checkInMethod, error: '',
    };
    setBookingState((previous) => ({ ...previous, ...bookingUpdates }));
    setUiState((previous) => ({ ...previous, isBookingFormOpen: false, bookingFormError: '' }));
    sendMessage(`Thông tin đặt phòng: nhận phòng ${form.checkIn}, trả phòng ${form.checkOut}; ${form.adults} người lớn, ${form.children} trẻ em; ${form.quantity} phòng; check-in bằng ${form.checkInMethod}.`, { bookingUpdates });
  };

  const usePersonalizedSuggestion = () => {
    const profile = bookingState.preference_profile || {};
    if (!profile.favoriteRoomType) return;
    updateForm({ adults: profile.usualAdults ?? uiState.bookingForm.adults, children: profile.usualChildren ?? uiState.bookingForm.children, checkInMethod: profile.preferredCheckIn || uiState.bookingForm.checkInMethod });
    selectRoom(profile.favoriteRoomType);
  };

  const clearChat = () => {
    const cleanState = createInitialBookingState();
    setBookingState(cleanState);
    setChatMessages([{ role: 'assistant', content: welcome, searchResults: null }]);
    setUiState({ isBookingFormOpen: false, bookingFormError: '', bookingForm: blankForm });
  };

  const value = useMemo(() => ({
    bookingState, chatMessages, uiState, inputText, isLoading,
    setInputText, sendMessage, selectRoom, updateForm, closeForm, submitBookingForm,
    usePersonalizedSuggestion, clearChat,
  }), [bookingState, chatMessages, uiState, inputText, isLoading]);
  return <AiChatContext.Provider value={value}>{children}</AiChatContext.Provider>;
}

export function useAiChat() {
  const context = useContext(AiChatContext);
  if (!context) throw new Error('useAiChat must be used within AiChatProvider');
  return context;
}
