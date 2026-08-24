import { useEffect, useRef } from 'react';
import { useLanguage } from '../context/LanguageContext';
import { useAiChat } from '../context/AiChatContext';

const chips = {
  VN: ['Các loại phòng tại The Iris Cần Thơ', 'Hướng dẫn check-in bằng khuôn mặt'],
  EN: ['What room types are available?', 'How does FaceID check-in work?'],
};

function BookingForm({ compact }) {
  const { uiState, updateForm, closeForm, submitBookingForm, isLoading } = useAiChat();
  const form = uiState.bookingForm;
  const today = new Date().toLocaleDateString('en-CA');
  const change = (field, amount, minimum) => updateForm({ [field]: Math.max(minimum, form[field] + amount) });
  const fields = [['adults', 'Người lớn', 1], ['children', 'Trẻ em', 0], ['quantity', 'Số phòng', 1]];
  return (
    <form onSubmit={submitBookingForm} className={`rounded-xl border border-primary/20 bg-red-50/40 ${compact ? 'p-3 space-y-3' : 'p-4 space-y-4'}`}>
      <div className="flex items-start justify-between gap-2"><div><p className="m-0 text-xs font-bold text-slate-800">Thông tin lưu trú</p><p className="m-0 mt-0.5 text-[10px] text-slate-500">Chọn trực tiếp, không cần nhập vào chat.</p></div><button type="button" onClick={closeForm} className="text-slate-500 hover:text-slate-800"><span className="material-symbols-outlined text-base">close</span></button></div>
      <div className="grid grid-cols-2 gap-2">
        <label className="text-[10px] font-bold text-slate-600">Ngày nhận<input required type="date" min={today} value={form.checkIn} onChange={(e) => updateForm({ checkIn: e.target.value })} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-2 py-2 text-xs text-slate-800 outline-none focus:border-primary" /></label>
        <label className="text-[10px] font-bold text-slate-600">Ngày trả<input required type="date" min={form.checkIn || today} value={form.checkOut} onChange={(e) => updateForm({ checkOut: e.target.value })} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-2 py-2 text-xs text-slate-800 outline-none focus:border-primary" /></label>
      </div>
      <div className="grid grid-cols-3 gap-2">{fields.map(([field, label, minimum]) => <div key={field} className="rounded-lg border border-slate-200 bg-white p-2"><p className="m-0 text-[10px] font-bold text-slate-600">{label}</p><div className="mt-1.5 flex items-center justify-between"><button type="button" disabled={form[field] <= minimum} onClick={() => change(field, -1, minimum)} className="h-6 w-6 rounded border border-slate-200 disabled:opacity-40">−</button><b className="text-xs">{form[field]}</b><button type="button" onClick={() => change(field, 1, minimum)} className="h-6 w-6 rounded border border-slate-200">+</button></div></div>)}</div>
      <div><p className="m-0 mb-1.5 text-[10px] font-bold text-slate-600">Hình thức check-in</p><div className="grid grid-cols-3 gap-1.5">{[['FaceID', 'face', 'FaceID'], ['QR Code', 'qr_code', 'QR Code'], ['Tại quầy', 'room_service', 'Manual']].map(([label, icon, value]) => <button key={value} type="button" onClick={() => updateForm({ checkInMethod: value })} className={`rounded-lg border px-1 py-2 text-[10px] font-bold ${form.checkInMethod === value ? 'border-primary bg-primary text-white' : 'border-slate-200 bg-white text-slate-700'}`}><span className="material-symbols-outlined mr-0.5 align-middle text-xs">{icon}</span>{label}</button>)}</div></div>
      {uiState.bookingFormError && <p className="m-0 text-[10px] font-semibold text-primary">{uiState.bookingFormError}</p>}
      <button type="submit" disabled={isLoading} className="w-full rounded-lg bg-primary py-2 text-[10px] font-bold uppercase tracking-wide text-white disabled:opacity-60">Tiếp tục với AI</button>
    </form>
  );
}

function MessageContent({ content, compact, suppressActions }) {
  const { sendMessage, selectRoom } = useAiChat();
  if (!content) return null;
  const hasRoomCards = content.includes('[ROOM_CARD:');
  const html = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  return html.split('\n').map((line, index) => {
    const text = line.trim();
    if (text.startsWith('[ROOM_CARD:')) {
      const match = text.match(/\[ROOM_CARD:\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\]/);
      if (!match) return null;
      const [, name, price, capacity, image] = match;
      return <div key={index} className={`my-2 overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm ${compact ? 'max-w-[245px]' : 'max-w-sm'}`}>{image && image !== 'null' && <img src={image} alt={name} className={compact ? 'h-24 w-full object-cover' : 'h-40 w-full object-cover'} />}<div className="p-3"><b className="text-slate-800">{name}</b><div className="mt-1 flex justify-between text-[10px] text-slate-600"><span>Sức chứa: {capacity}</span><b className="text-primary">{price}</b></div><button onClick={() => selectRoom(name)} className="mt-2 w-full rounded-md border border-slate-200 bg-slate-50 py-2 text-[10px] font-bold text-slate-800 hover:border-primary hover:text-primary">Chọn phòng này</button></div></div>;
    }
    if (text.startsWith('[ACTIONS:')) {
      if (suppressActions || hasRoomCards) return null;
      const match = text.match(/\[ACTIONS:\s*(.*?)\]/);
      return match ? <div key={index} className="my-2 flex flex-wrap gap-1.5">{match[1].split('|').map((action) => <button key={action} onClick={() => sendMessage(action.trim())} className="rounded-full border border-slate-200 bg-white px-2.5 py-1.5 text-[10px] font-bold text-slate-700">{action.trim()}</button>)}</div> : null;
    }
    if (!text) return <div key={index} className="h-1.5" />;
    const list = text.match(/^[-•]\s+(.*)/) || text.match(/^\d+\.\s+(.*)/);
    const Tag = list ? 'li' : 'p';
    return <Tag key={index} className={list ? 'ml-4 my-1 list-disc' : 'my-1'} dangerouslySetInnerHTML={{ __html: list ? list[1] : text }} />;
  });
}

export default function AiChatConversation({ compact = false }) {
  const { language } = useLanguage();
  const { chatMessages, uiState, inputText, setInputText, isLoading, sendMessage, bookingState, usePersonalizedSuggestion } = useAiChat();
  const scrollRef = useRef(null);
  useEffect(() => { scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' }); }, [chatMessages, isLoading, uiState.isBookingFormOpen]);
  const profile = bookingState.preference_profile || {};
  return <div className="flex min-h-0 flex-1 flex-col">
    <div ref={scrollRef} className={`min-h-0 flex-1 overflow-y-auto ${compact ? 'p-3 space-y-3' : 'p-6 space-y-6'} bg-slate-50`}>
      {chatMessages.map((message, index) => {
        const bot = message.role === 'assistant';
        const latestBot = bot && !chatMessages.slice(index + 1).some((item) => item.role === 'assistant');
        return <div key={index} className={`flex ${bot ? 'justify-start' : 'justify-end'}`}><div className={`flex gap-2 ${compact ? 'max-w-[92%]' : 'max-w-[85%]'} ${!bot ? 'flex-row-reverse' : ''}`}><div className={`${compact ? 'h-6 w-6 text-[10px]' : 'h-9 w-9'} flex shrink-0 items-center justify-center rounded-full ${bot ? 'bg-primary text-white' : 'border border-slate-200 bg-white text-slate-700'}`}><span className={`material-symbols-outlined ${compact ? 'text-xs' : 'text-[18px]'}`}>{bot ? 'support_agent' : 'person'}</span></div><div className={`${compact ? 'p-3 text-[11px]' : 'p-4 text-[14px]'} rounded-2xl ${bot ? 'rounded-tl-sm border border-slate-200 bg-white text-slate-800 shadow-sm' : 'rounded-tr-sm bg-neutral-900 text-white shadow-md'}`}>{bot ? <div className="space-y-1 leading-relaxed"><MessageContent content={message.content} compact={compact} suppressActions={uiState.isBookingFormOpen && latestBot} /></div> : <p className="m-0 whitespace-pre-wrap">{message.content}</p>}{bot && uiState.isBookingFormOpen && latestBot && <div className="mt-3 border-t border-slate-100 pt-3"><BookingForm compact={compact} /></div>}</div></div></div>;
      })}
      {isLoading && <div className="flex gap-2"><div className={`${compact ? 'h-6 w-6' : 'h-9 w-9'} flex items-center justify-center rounded-full bg-primary text-white`}><span className="material-symbols-outlined animate-spin text-sm">sync</span></div><div className="flex items-center gap-1 rounded-2xl rounded-tl-sm border border-slate-200 bg-white px-4"><i className="h-1.5 w-1.5 animate-bounce rounded-full bg-primary" /><i className="h-1.5 w-1.5 animate-bounce rounded-full bg-primary" /><i className="h-1.5 w-1.5 animate-bounce rounded-full bg-primary" /></div></div>}
    </div>
    {!uiState.isBookingFormOpen && <div className={`border-t border-slate-100 bg-white ${compact ? 'p-2' : 'p-4'}`}>{bookingState.step === 'idle' && profile.hasHistory && profile.favoriteRoomType && <button onClick={usePersonalizedSuggestion} className="mb-2 w-full rounded-lg border border-primary/20 bg-red-50 px-2 py-2 text-left text-[10px] font-bold text-primary">Gợi ý dành cho bạn: {profile.favoriteRoomType} · Dùng gợi ý</button>}<div className="flex gap-1.5 overflow-x-auto">{(chips[language] || chips.EN).map((chip) => <button key={chip} onClick={() => sendMessage(chip)} disabled={isLoading} className="shrink-0 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-[10px] font-bold text-slate-600">{chip}</button>)}</div></div>}
    <form onSubmit={(event) => { event.preventDefault(); sendMessage(); }} className={`flex gap-2 border-t border-slate-100 bg-white ${compact ? 'p-2' : 'p-4'}`}><textarea value={inputText} onChange={(e) => setInputText(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); } }} disabled={isLoading} placeholder={language === 'VN' ? 'Nhập phản hồi của bạn...' : 'Type your reply...'} rows={1} className={`min-h-[40px] flex-1 resize-none rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium outline-none focus:border-primary`} /><button type="submit" disabled={isLoading || !inputText.trim()} className="h-10 w-10 rounded-xl bg-neutral-900 text-white disabled:bg-slate-200"><span className="material-symbols-outlined text-base">arrow_upward</span></button></form>
  </div>;
}
