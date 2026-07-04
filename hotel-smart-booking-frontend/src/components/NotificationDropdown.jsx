import { useState, useEffect, useRef } from 'react';
import { Bell, Calendar, CreditCard, RefreshCw, Smartphone, Plus, LogOut, CheckCircle2, MessageSquare, Check, CheckSquare } from 'lucide-react';
import { getNotifications, markAsRead, markAllAsRead, getUnreadCount } from '../services/notificationService';
import { useToast } from './Toast';

export default function NotificationDropdown({ dark = false, onNewNotification }) {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const dropdownRef = useRef(null);
  const { showToast } = useToast();

  const isLoggedIn = !!localStorage.getItem('accessToken');

  const fetchNotifications = async () => {
    try {
      const res = await getNotifications(false, 0, 15);
      if (res && res.success && res.data) {
        setNotifications(res.data.content || []);
      }
    } catch (err) {
      console.error("Failed to fetch notifications:", err);
    }
  };

  const fetchUnreadCount = async () => {
    try {
      const res = await getUnreadCount();
      if (res && res.success) {
        setUnreadCount(res.data);
      }
    } catch (err) {
      console.error("Failed to fetch unread count:", err);
    }
  };

  useEffect(() => {
    if (isLoggedIn) {
      fetchNotifications();
      fetchUnreadCount();
    }
  }, [isLoggedIn]);

  // STOMP over WebSocket client connection
  useEffect(() => {
    if (!isLoggedIn) return;

    let socket = null;
    
    const connectWS = () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (!token) return;

        socket = new WebSocket('ws://localhost:8080/ws/websocket');
        
        socket.onopen = () => {
          socket.send(`CONNECT\naccept-version:1.1,1.2\nAuthorization:Bearer ${token}\n\n\x00`);
        };
        
        socket.onmessage = (event) => {
          const raw = event.data;
          if (raw.startsWith("CONNECTED")) {
            socket.send("SUBSCRIBE\nid:sub-notifications\ndestination:/user/queue/notifications\n\n\x00");
            console.log('Notification WebSocket connected and subscribed.');
          } else if (raw.includes("/user/queue/notifications")) {
            const bodyStart = raw.indexOf('{');
            const bodyEnd = raw.lastIndexOf('}');
            if (bodyStart !== -1 && bodyEnd !== -1) {
              try {
                const bodyStr = raw.substring(bodyStart, bodyEnd + 1);
                const newNotif = JSON.parse(bodyStr);
                
                // Add to list and show toast
                setNotifications(prev => [newNotif, ...prev]);
                setUnreadCount(c => c + 1);
                
                if (onNewNotification) {
                  onNewNotification(newNotif);
                } else {
                  showToast(`${newNotif.title}: ${newNotif.message}`, 'info');
                }
              } catch (ex) {
                console.error('Error parsing WS notification:', ex);
              }
            }
          }
        };
        
        socket.onerror = (err) => {
          console.error('Notification WS error:', err);
        };
        
        socket.onclose = () => {
          console.log('Notification WS connection closed. Reconnecting in 5s...');
          setTimeout(connectWS, 5000);
        };
      } catch (e) {
        console.error('Failed to create notification WS connection:', e);
      }
    };

    connectWS();
    
    return () => {
      if (socket) {
        socket.onclose = null;
        socket.close();
      }
    };
  }, [isLoggedIn]);

  // Click outside to close dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleToggle = () => {
    setIsOpen(!isOpen);
    if (!isOpen) {
      fetchNotifications();
      fetchUnreadCount();
    }
  };

  const handleMarkAsRead = async (id, event) => {
    event.stopPropagation();
    try {
      const res = await markAsRead(id);
      if (res && res.success) {
        setNotifications(prev =>
          prev.map(n => n.id === id ? { ...n, isread: true } : n)
        );
        setUnreadCount(c => Math.max(0, c - 1));
      }
    } catch (err) {
      console.error("Failed to mark notification as read:", err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const res = await markAllAsRead();
      if (res && res.success) {
        setNotifications(prev => prev.map(n => ({ ...n, isread: true })));
        setUnreadCount(0);
        showToast("Đã đánh dấu tất cả thông báo là đã đọc", "success");
      }
    } catch (err) {
      console.error("Failed to mark all as read:", err);
    }
  };

  const formatRelativeTime = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHr = Math.floor(diffMin / 60);
    const diffDays = Math.floor(diffHr / 24);

    if (diffSec < 60) return "Vừa xong";
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHr < 24) return `${diffHr} giờ trước`;
    if (diffDays === 1) return "Hôm qua";
    return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const getIcon = (type) => {
    const size = 16;
    switch (type) {
      case 'Booking':
        return <Calendar size={size} className="text-blue-500" />;
      case 'Payment':
        return <CreditCard size={size} className="text-emerald-500" />;
      case 'Refund':
        return <RefreshCw size={size} className="text-orange-500" />;
      case 'RoomChange':
        return <Smartphone size={size} className="text-purple-500" />;
      case 'StayExtension':
        return <Plus size={size} className="text-pink-500" />;
      case 'EarlyCheckOut':
        return <LogOut size={size} className="text-rose-500" />;
      case 'Ekyc':
        return <CheckCircle2 size={size} className="text-cyan-500" />;
      default:
        return <MessageSquare size={size} className="text-slate-500" />;
    }
  };

  if (!isLoggedIn) return null;

  return (
    <div className="relative inline-block text-left" ref={dropdownRef}>
      {/* Trigger Button */}
      <button
        onClick={handleToggle}
        className={`relative p-2.5 rounded-full transition-all duration-200 focus:outline-none cursor-pointer flex items-center justify-center ${
          dark 
            ? 'text-slate-400 hover:text-white hover:bg-neutral-800/50' 
            : 'text-slate-600 hover:text-primary hover:bg-slate-100'
        }`}
      >
        <Bell size={dark ? 18 : 20} />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[8px] font-extrabold text-white animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Popover */}
      {isOpen && (
        <div
          className={`absolute right-0 mt-3 w-80 sm:w-96 shadow-2xl rounded-lg border overflow-hidden z-[100] transition-all transform origin-top-right duration-200 ${
            dark 
              ? 'bg-[#121214]/98 border-neutral-800 text-white backdrop-blur-md' 
              : 'bg-white/98 border-slate-200/80 text-slate-800 backdrop-blur-md'
          }`}
        >
          {/* Header */}
          <div className={`flex items-center justify-between px-4 py-3 border-b ${dark ? 'border-neutral-800/80' : 'border-slate-100'}`}>
            <h3 className="text-xs font-black uppercase tracking-wider m-0">Thông báo</h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllAsRead}
                className="text-[10px] font-bold text-primary hover:underline cursor-pointer border-none bg-transparent flex items-center gap-1"
              >
                <CheckSquare size={10} />
                Đọc tất cả
              </button>
            )}
          </div>

          {/* List */}
          <div className="max-h-[360px] overflow-y-auto divide-y divide-solid divide-slate-100 dark:divide-neutral-800/50 scrollbar-thin">
            {notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 px-4 text-center">
                <Bell size={28} className="opacity-30 mb-2" />
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Không có thông báo mới</p>
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  className={`p-4 flex gap-3 transition-colors relative group select-none ${
                    n.isread 
                      ? 'bg-transparent' 
                      : dark 
                        ? 'bg-primary/5 hover:bg-primary/10' 
                        : 'bg-primary/5 hover:bg-primary/10'
                  } ${
                    dark ? 'hover:bg-neutral-900/40 border-neutral-900/20' : 'hover:bg-slate-50 border-slate-100/50'
                  }`}
                >
                  {/* Icon Column */}
                  <div className={`size-8 rounded-full flex items-center justify-center flex-shrink-0 ${dark ? 'bg-neutral-800' : 'bg-slate-100'}`}>
                    {getIcon(n.type)}
                  </div>

                  {/* Content Column */}
                  <div className="flex-1 min-w-0 pr-6 text-left">
                    <h4 className="text-[11px] font-extrabold m-0 tracking-wide leading-snug">{n.title}</h4>
                    <p className={`text-[10px] mt-1 mb-0 leading-normal ${dark ? 'text-slate-300' : 'text-slate-600'}`}>
                      {n.message}
                    </p>
                    <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block mt-2">
                      {formatRelativeTime(n.sentat)}
                    </span>
                  </div>

                  {/* Unread dot / Mark read check */}
                  {!n.isread && (
                    <button
                      onClick={(e) => handleMarkAsRead(n.id, e)}
                      title="Đánh dấu đã đọc"
                      className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity duration-150 p-1 rounded-full cursor-pointer hover:bg-slate-200 dark:hover:bg-neutral-800 border-none bg-transparent flex items-center justify-center"
                    >
                      <Check size={12} className="text-primary" />
                    </button>
                  )}
                  {!n.isread && (
                    <span className="absolute right-4 top-4 size-2 rounded-full bg-primary group-hover:opacity-0 transition-opacity" />
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
