import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Profile from '../pages/Profile';
import ChangePassword from '../pages/ChangePassword';
import { useToast, ToastContainer } from './Toast';
import NotificationDropdown from './NotificationDropdown';
import { getUserProfile } from '../services/userService';
import { getRoomTypes, getRoomTypeDetail } from '../services/roomService';
import RoomDetailModern from './room/RoomDetailModern';
import { useLanguage } from '../context/LanguageContext';

export default function Navbar({ activePage, setActivePage, isMobileMenuOpen, setIsMobileMenuOpen }) {
  const { language, setLanguage, t } = useLanguage();
  const [menuState, setMenuState] = useState('idle'); // 'idle' | 'open' | 'closed'
  const [isScrolled, setIsScrolled] = useState(false);
  const [hoveredId, setHoveredId] = useState(null);
  const [underlineStyle, setUnderlineStyle] = useState({ left: 0, width: 0, opacity: 0 });
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [activeModal, setActiveModal] = useState(null); // 'profile' | null
  const [roomTypes, setRoomTypes] = useState([]);
  const [showRoomTypes, setShowRoomTypes] = useState(false);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [prevRoom, setPrevRoom] = useState(null);
  const [slideDirection, setSlideDirection] = useState('right'); // 'left' | 'right'
  const [roomDetailData, setRoomDetailData] = useState(null);
  const [isLoadingRoomDetail, setIsLoadingRoomDetail] = useState(false);

  const navContainerRef = useRef(null);
  const dropdownRef = useRef(null);
  const langDropdownRef = useRef(null);
  const itemRefs = useRef({});
  const navigate = useNavigate();
  const { toasts, showToast, dismissToast } = useToast();

  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

  const languagesList = [
    { code: 'VN', label: 'Tiếng Việt', flag: 'https://flagcdn.com/w40/vn.png' },
    { code: 'EN', label: 'English', flag: 'https://flagcdn.com/w40/us.png' },
    { code: 'JP', label: '日本語', flag: 'https://flagcdn.com/w40/jp.png' },
    { code: 'KR', label: '한국어', flag: 'https://flagcdn.com/w40/kr.png' },
    { code: 'CN', label: '简体中文', flag: 'https://flagcdn.com/w40/cn.png' }
  ];

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
      if (langDropdownRef.current && !langDropdownRef.current.contains(event.target)) {
        setIsLangDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem('accessToken'));
  const [currentUser, setCurrentUser] = useState(() => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  });

  useEffect(() => {
    setIsLoggedIn(!!localStorage.getItem('accessToken'));
    const userStr = localStorage.getItem('user');
    setCurrentUser(userStr ? JSON.parse(userStr) : null);
  }, [activePage]);

  const avatarUrl = currentUser?.avatar || currentUser?.avatarUrl || 'https://i.pravatar.cc/36?img=12';
  const fullName = currentUser?.fullName || currentUser?.name || 'Hội viên Elysian';

  // Fetch fresh profile data from API on login/load and modal opening
  useEffect(() => {
    if (isLoggedIn) {
      const fetchProfile = async () => {
        try {
          const response = await getUserProfile();
          if (response && response.data) {
            setCurrentUser(response.data);
            localStorage.setItem('user', JSON.stringify(response.data));
          }
        } catch (err) {
          console.error('Lỗi khi tải thông tin hồ sơ từ server:', err);
        }
      };
      fetchProfile();
    }
  }, [isLoggedIn, activeModal]);

  // Fetch room types data on load
  useEffect(() => {
    const fetchRooms = async () => {
      try {
        const response = await getRoomTypes();
        if (response && response.data && response.data.content) {
          setRoomTypes(response.data.content);
          setSelectedRoom(response.data.content[0]);
        }
      } catch (err) {
        console.error('Lỗi khi tải danh sách loại phòng:', err);
      }
    };
    fetchRooms();
  }, []);

  // Sync prevRoom for transition animation
  useEffect(() => {
    if (selectedRoom) {
      if (!prevRoom) {
        setPrevRoom(selectedRoom);
      } else {
        const timer = setTimeout(() => {
          setPrevRoom(selectedRoom);
        }, 800);
        return () => clearTimeout(timer);
      }
    }
  }, [selectedRoom, prevRoom]);

  const handleNextRoom = (e) => {
    e.stopPropagation();
    if (roomTypes.length === 0) return;
    const currentIndex = roomTypes.findIndex(r => r.id === selectedRoom?.id);
    const nextIndex = currentIndex === -1 ? 0 : (currentIndex + 1) % roomTypes.length;
    setSlideDirection('right'); // Flow from left to right (towards the right side pressed)
    setSelectedRoom(roomTypes[nextIndex]);
  };

  const handlePrevRoom = (e) => {
    e.stopPropagation();
    if (roomTypes.length === 0) return;
    const currentIndex = roomTypes.findIndex(r => r.id === selectedRoom?.id);
    const prevIndex = currentIndex === -1 ? 0 : (currentIndex - 1 + roomTypes.length) % roomTypes.length;
    setSlideDirection('left'); // Flow from right to left (towards the left side pressed)
    setSelectedRoom(roomTypes[prevIndex]);
  };
  const handleOpenRoomDetail = async (roomId) => {
    if (!roomId) return;
    setIsLoadingRoomDetail(true);
    setActiveModal('roomDetail');
    try {
      const response = await getRoomTypeDetail(roomId);
      if (response && response.data) {
        setRoomDetailData(response.data);
      }
    } catch (err) {
      console.error('Lỗi khi tải chi tiết phòng:', err);
      showToast('Không thể tải thông tin chi tiết phòng.', 'error');
      setActiveModal(null);
    } finally {
      setIsLoadingRoomDetail(false);
    }
  };

  const navItems = [
    { id: 'home', label: t('nav_hotels') },
    { id: 'residences', label: t('nav_residences') },
    { id: 'experiences', label: t('nav_experiences') },
    { id: 'events', label: t('nav_events') },
    { id: 'offers', label: t('nav_offers') },
    { id: 'ai-assistant', label: t('nav_ai_assistant', 'TRỢ LÝ AI') }
  ];

  const handleNavClick = (id) => {
    setActivePage(id);
    if (isMobileMenuOpen) {
      setMenuState('closed');
      setIsMobileMenuOpen(false);
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const toggleMenu = () => {
    if (isMobileMenuOpen) {
      setMenuState('closed');
      setIsMobileMenuOpen(false);
    } else {
      setMenuState('open');
      setIsMobileMenuOpen(true);
    }
  };

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 20) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  useEffect(() => {
    const activeElement = itemRefs.current[activePage];
    if (activeElement) {
      setUnderlineStyle({
        left: activeElement.offsetLeft,
        width: activeElement.offsetWidth,
        opacity: 0
      });
    }
  }, [activePage]);

  useEffect(() => {
    const updateUnderline = () => {
      if (hoveredId) {
        const hoveredElement = itemRefs.current[hoveredId];
        if (hoveredElement) {
          setUnderlineStyle({
            left: hoveredElement.offsetLeft,
            width: hoveredElement.offsetWidth,
            opacity: 1
          });
        }
      } else {
        setUnderlineStyle(prev => ({ ...prev, opacity: 0 }));
      }
    };

    updateUnderline();

    window.addEventListener('resize', updateUnderline);
    const timer = setTimeout(updateUnderline, 50);

    return () => {
      window.removeEventListener('resize', updateUnderline);
      clearTimeout(timer);
    };
  }, [hoveredId]);

  // Lock body scroll when drawer is open
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isMobileMenuOpen]);

  return (
    <>
      <header className={`fixed top-0 left-0 right-0 z-50 transition-transform duration-300 ease-in-out ${
        isScrolled ? '-translate-y-12 shadow-md' : 'translate-y-0'
      }`}>
      {/* Top Black Bar (Height: h-12 / 48px) */}
      <div className="bg-black text-white h-12 flex justify-between items-center px-4 md:px-margin-desktop relative">
        <div className="flex-1"></div>
        {/* Brand Logo centered */}
        <div className="absolute left-1/2 -translate-x-1/2 flex flex-col items-center justify-center h-full pt-1">
          <button 
            onClick={() => handleNavClick('home')} 
            className="font-headline-lg text-lg font-black tracking-widest hover:text-primary transition-colors cursor-pointer bg-transparent border-none p-0 text-white leading-none"
          >
            ELYSIAN
          </button>
          <span className="text-[6.5px] tracking-[0.25em] opacity-60 uppercase font-semibold leading-none mt-0.5">HOTELS</span>
        </div>
        {/* Language selector on the right with multi-language flag dropdown */}
        <div className="flex-1 flex justify-end relative" ref={langDropdownRef}>
          {(() => {
            const activeLangObj = languagesList.find(l => l.code === language) || languagesList[0];
            return (
              <>
                <button 
                  onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                  className="flex items-center gap-1.5 text-white hover:text-primary cursor-pointer bg-transparent border-none py-1 px-2 hover:bg-white/10 transition-colors"
                >
                  <img 
                    src={activeLangObj.flag} 
                    alt={activeLangObj.label} 
                    className="w-5 h-3.5 object-cover border border-white/20"
                  />
                  <span className="text-[10px] font-bold tracking-wider opacity-85 uppercase">{activeLangObj.code}</span>
                  <span className="material-symbols-outlined text-sm leading-none opacity-80">arrow_drop_down</span>
                </button>

                {isLangDropdownOpen && (
                  <div className="absolute right-0 top-10 w-40 bg-[#0a0a0c] border border-neutral-900 shadow-2xl z-50 py-1.5 font-['Montserrat']">
                    {languagesList.map((lang) => (
                      <button
                        key={lang.code}
                        onClick={() => {
                          setLanguage(lang.code);
                          setIsLangDropdownOpen(false);
                        }}
                        className={`w-full flex items-center gap-3 px-4 py-2.5 text-left text-[10px] font-bold uppercase tracking-wider transition-colors hover:bg-white/5 border-none bg-transparent cursor-pointer ${
                          language === lang.code ? 'text-primary' : 'text-slate-300'
                        }`}
                      >
                        <img 
                          src={lang.flag} 
                          alt={lang.label} 
                          className="w-5 h-3.5 object-cover border border-white/10"
                        />
                        <span>{lang.label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </>
            );
          })()}
        </div>
      </div>

      {/* Main White Nav Bar (Height: h-12 / 48px to match black bar) */}
      <div className="bg-white border-b border-outline-variant h-12 flex justify-between items-center pl-4 md:pl-margin-desktop pr-0 relative">
        <div className="flex items-center h-full">
          {/* Custom Animated Hamburger Button */}
          <button 
            className={`burger-menu ${menuState === 'open' ? 'open' : menuState === 'closed' ? 'closed' : ''}`}
            onClick={toggleMenu}
            aria-label="Toggle navigation menu"
          >
            <div className="burger-bar"></div>
            <div className="burger-bar"></div>
            <div className="burger-bar"></div>
          </button>
        </div>

        {/* Desktop Navigation (Centered; middle item aligns cleanly under logo) */}
        <nav ref={navContainerRef} className="hidden md:flex gap-6 mx-auto justify-center items-center h-full relative">
          {navItems.map((item) => (
            <button
              key={item.id}
              ref={(el) => { itemRefs.current[item.id] = el; }}
              onClick={() => handleNavClick(item.id)}
              onMouseEnter={() => setHoveredId(item.id)}
              onMouseLeave={() => setHoveredId(null)}
              className={`uppercase transition-colors duration-300 h-full cursor-pointer bg-transparent border-none font-bold text-[10.5px] tracking-tight flex items-center relative ${
                activePage === item.id 
                  ? 'text-primary' 
                  : 'text-on-surface hover:text-primary'
              }`}
            >
              {item.label}
              {/* Static Underline for Active Item */}
              {activePage === item.id && (
                <div className="absolute bottom-0 left-0 right-0 h-[3px] bg-primary" />
              )}
            </button>
          ))}
          {/* Sliding Underline */}
          <div 
            className="absolute bottom-0 h-[3px] bg-primary pointer-events-none left-0 origin-left"
            style={{
              width: '1px',
              transform: `translateX(${underlineStyle.left}px) scaleX(${underlineStyle.width})`,
              opacity: underlineStyle.opacity,
              transformOrigin: 'left',
              transition: 'transform 140ms cubic-bezier(0.25, 1, 0.5, 1), opacity 140ms ease-out'
            }}
          />
        </nav>

        {/* Book Now / Profile Dropdown */}
        <div className="h-full flex items-center pr-6 relative gap-2">
          {isLoggedIn && (
            <NotificationDropdown dark={false} onNewNotification={(n) => {
              showToast(`${n.title}: ${n.message}`, 'info');
            }} />
          )}
          {isLoggedIn ? (
            <div className="relative h-full flex items-center" ref={dropdownRef}>
              <button
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                className="flex items-center gap-3 px-3 py-1.5 hover:bg-slate-100 transition-colors duration-150 cursor-pointer border-none bg-transparent h-full"
              >
                <img
                  src={avatarUrl}
                  alt={fullName}
                  className="size-8 rounded-full border border-primary object-cover"
                />
                <span className="text-[10px] font-bold text-slate-800 uppercase tracking-wider hidden sm:inline">
                  {fullName}
                </span>
                <span className="material-symbols-outlined text-sm leading-none opacity-80">arrow_drop_down</span>
              </button>

              {/* Dropdown Menu */}
              {isDropdownOpen && (
                <div className="absolute right-0 top-12 w-72 bg-white text-slate-800 shadow-2xl border border-outline-variant z-50 p-4 font-['Montserrat'] select-none rounded-none">
                  {/* Dropdown Header */}
                  <div className="flex items-center gap-3 mb-4">
                    <img
                      src={avatarUrl}
                      alt={fullName}
                      className="size-12 rounded-full border-2 border-primary object-cover"
                    />
                    <div className="overflow-hidden">
                      <h4 className="text-xs font-bold text-slate-900 truncate m-0 uppercase tracking-wider">{fullName}</h4>
                    </div>
                  </div>

                  {/* Primary view profile action */}
                  <button
                    onClick={() => {
                      navigate('/dashboard');
                      setIsDropdownOpen(false);
                    }}
                    className="w-full py-2.5 bg-primary-container text-on-primary font-bold text-[10px] tracking-widest uppercase hover:brightness-110 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 rounded-none"
                  >
                    <span className="material-symbols-outlined text-sm">account_circle</span>
                    Xem tất cả trang cá nhân
                  </button>
 
                  <div className="h-px bg-slate-200 my-3" />
 
                  {/* Options */}
                  <div className="space-y-1">
                    <button
                      onClick={() => {
                        showToast('Đăng xuất thành công!', 'success');
                        localStorage.removeItem('accessToken');
                        localStorage.removeItem('refreshToken');
                        localStorage.removeItem('user');
                        setIsLoggedIn(false);
                        setCurrentUser(null);
                        setActivePage('home');
                        navigate('/');
                        setIsDropdownOpen(false);
                      }}
                      className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-[10px] font-bold uppercase tracking-widest text-primary hover:bg-[#ffe0dd]/30 transition-colors cursor-pointer border-none bg-transparent rounded-none"
                    >
                      <span className="material-symbols-outlined text-base">logout</span>
                      <span>Đăng xuất</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <button 
              onClick={() => handleNavClick('login')}
              className="parallelogram-btn bg-primary-container text-on-primary h-full px-14 font-bold text-xs uppercase tracking-wider active:scale-98 transition-all duration-150 cursor-pointer border-none flex items-center justify-center"
            >
              {t('nav_book_now')}
            </button>
          )}
        </div>
      </div>
    </header>
      
    {/* Drawer Navigation - outside header to cover full landing page correctly */}
    <div className={`fixed left-0 right-0 bottom-0 bg-background z-10 overflow-y-auto drawer-menu ${
      isScrolled ? 'top-12' : 'top-24'
    } ${
      isMobileMenuOpen ? 'open' : ''
    }`}>
        <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-12 p-10 pl-4 md:pl-6 gap-8 min-h-full items-start">
          
          {/* Left Column: Menu Items & Secondary Links (col-span-4) */}
          <div className="md:col-span-4 flex flex-col justify-between min-h-[420px] py-4 self-start">
            {/* Main Navigation List */}
            <div className="flex flex-col space-y-7 text-left font-['Montserrat'] select-none">

              {/* LOẠI PHÒNG (Toggles room types list underneath) */}
              <div className="flex flex-col text-left">
                <button
                  onClick={() => {
                    setShowRoomTypes(!showRoomTypes);
                  }}
                  className="group text-left font-bold text-[12.5px] md:text-[14px] uppercase tracking-widest cursor-pointer bg-transparent border-none w-fit transition-all duration-300"
                >
                  <span className={`relative pb-1 transition-colors duration-300 font-extrabold ${
                    (showRoomTypes || activePage === 'home') ? 'text-primary' : 'text-slate-800 group-hover:text-primary'
                  }`}>
                    {t('nav_room_types')}
                    <span className={`absolute bottom-0 left-0 right-0 h-[2.5px] bg-primary transition-transform duration-300 origin-left ${
                      (showRoomTypes || activePage === 'home') ? 'scale-x-100' : 'scale-x-0 group-hover:scale-x-100'
                    }`} />
                  </span>
                </button>
                
                {/* Expandable sub-list of room names */}
                {showRoomTypes && roomTypes.length > 0 && (
                  <div className="pl-5 mt-3.5 flex flex-col space-y-3 border-l border-slate-300">
                    {roomTypes.map((room) => (
                      <button
                        key={room.id}
                        onMouseEnter={() => {
                          const currentIndex = roomTypes.findIndex(r => r.id === selectedRoom?.id);
                          const newIndex = roomTypes.findIndex(r => r.id === room.id);
                          if (newIndex > currentIndex) {
                            setSlideDirection('right');
                          } else if (newIndex < currentIndex) {
                            setSlideDirection('left');
                          }
                          setSelectedRoom(room);
                        }}
                        onClick={() => {
                          setIsMobileMenuOpen(false);
                          setActivePage('home');
                        }}
                        className={`text-left text-[11px] font-bold uppercase tracking-wider cursor-pointer bg-transparent border-none transition-all duration-200 hover:text-primary ${
                          selectedRoom?.id === room.id ? 'text-primary font-extrabold translate-x-0.5' : 'text-slate-600'
                        }`}
                      >
                        {room.name}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Other items */}
              {navItems.slice(1).map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    setShowRoomTypes(false);
                    handleNavClick(item.id);
                  }}
                  className="group text-left font-bold text-[12.5px] md:text-[14px] uppercase tracking-widest cursor-pointer bg-transparent border-none w-fit transition-all duration-300"
                >
                  <span className={`relative pb-1 transition-colors duration-300 font-extrabold ${
                    (activePage === item.id && !showRoomTypes) ? 'text-primary' : 'text-slate-800 group-hover:text-primary'
                  }`}>
                    {item.label}
                    <span className={`absolute bottom-0 left-0 right-0 h-[2.5px] bg-primary transition-transform duration-300 origin-left ${
                      (activePage === item.id && !showRoomTypes) ? 'scale-x-100' : 'scale-x-0 group-hover:scale-x-100'
                    }`} />
                  </span>
                </button>
              ))}
            </div>

            {/* Bottom Left Secondary Links */}
            <div className="grid grid-cols-2 gap-y-3.5 gap-x-4 text-[10px] font-bold text-slate-500 uppercase tracking-widest mt-12 select-none border-t border-slate-200 pt-6">
              <button onClick={() => handleNavClick('group-booking')} className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider text-primary">{t('nav_group_booking')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_about')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_careers')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_environment')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_offers')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_blogs')}</button>
              <button className="text-left hover:text-primary transition-all duration-200 hover:translate-x-0.5 cursor-pointer bg-transparent border-none p-0 font-bold uppercase text-[10px] tracking-wider">{t('nav_contact')}</button>
            </div>
          </div>

          {/* Right Column: Dynamic Room Image Carousel (col-span-8) */}
          <div className="md:col-span-8 pl-0 md:pl-8 flex flex-col justify-start self-start py-4 w-full">
            <div className="w-full h-[450px] overflow-hidden relative shadow-2xl border border-slate-200 group bg-slate-950">
              
              {/* Background Layer: Previous Room Image */}
              {prevRoom && prevRoom.id !== selectedRoom?.id && (
                <div className="absolute inset-0 w-full h-full overflow-hidden">
                  <img 
                    src={prevRoom?.primaryImageUrl} 
                    alt={prevRoom?.name} 
                    className="w-full h-full object-cover opacity-100 scale-100 transition-all duration-700"
                  />
                  {/* Dark Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />
                </div>
              )}

              {/* Foreground Layer: Selected Room Image */}
              <div 
                key={selectedRoom?.id} 
                className={`absolute inset-0 w-full h-full overflow-hidden ${
                  slideDirection === 'left' ? 'animate-room-wipe-left' : 'animate-room-wipe-right'
                }`}
              >
                <img 
                  src={selectedRoom?.primaryImageUrl || (roomTypes[0]?.primaryImageUrl) || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1000&q=80'} 
                  alt={selectedRoom?.name || "Elysian Room"} 
                  className="w-full h-full object-cover animate-room-zoom"
                />
                
                {/* Dark Gradient Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />

                {/* Room Name & Info Overlay (Bottom Left) */}
                <div className="absolute bottom-8 left-8 text-left z-10 animate-room-text">
                  <span className="text-[10px] font-black text-white/80 uppercase tracking-widest block mb-1">
                    {selectedRoom?.bedType || 'ELYSIAN HOTELS & RESORTS'}
                  </span>
                  <h3 className="text-2xl font-black text-white uppercase tracking-wider leading-tight">
                    {selectedRoom?.name || 'KỲ NGHỈ DƯỠNG THƯỢNG LƯU'}
                  </h3>
                  {selectedRoom && (
                    <p className="text-xs font-bold text-white/95 uppercase tracking-widest mt-1.5">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedRoom.basePrice)} / ĐÊM
                    </p>
                  )}
                </div>

                {/* Chi tiết Button (Bottom Right) */}
                {selectedRoom && (
                  <div className="absolute bottom-8 right-8 z-10 animate-room-btn">
                    <button
                      onClick={() => handleOpenRoomDetail(selectedRoom.id)}
                      className="bg-transparent text-white/95 hover:text-white transition-all duration-300 text-base border-b border-white/40 pb-1 cursor-pointer hover:border-white whitespace-nowrap"
                      style={{ fontFamily: "'Playfair Display', serif" }}
                    >
                      {t('nav_detail')}
                    </button>
                  </div>
                )}
              </div>

              {/* Left/Right Carousel Controls */}
              {roomTypes && roomTypes.length > 1 && (
                <>
                  <button 
                    onClick={handlePrevRoom}
                    className="absolute left-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/30 hover:bg-black/60 text-white flex items-center justify-center transition-all cursor-pointer border-none z-20"
                    aria-label="Previous room"
                  >
                    <span className="material-symbols-outlined text-2xl">chevron_left</span>
                  </button>
                  <button 
                    onClick={handleNextRoom}
                    className="absolute right-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/30 hover:bg-black/60 text-white flex items-center justify-center transition-all cursor-pointer border-none z-20"
                    aria-label="Next room"
                  >
                    <span className="material-symbols-outlined text-2xl">chevron_right</span>
                  </button>
                </>
              )}
            </div>
          </div>

        </div>
      </div>

    {/* Modal Dialog for Profiles - outside of translating header element */}
    {activeModal === 'profile' && (
      <div className="fixed inset-0 bg-black/65 backdrop-blur-sm z-[9999] flex justify-center items-start overflow-y-auto p-4 md:p-8">
        <div className="bg-white rounded-none max-w-5xl w-full my-4 md:my-8 shadow-2xl relative border border-outline-variant animate-scale-in">
          {/* Close Button */}
          <button
            onClick={() => setActiveModal(null)}
            className="absolute top-6 right-6 p-2 text-slate-600 hover:text-white hover:bg-primary transition-all border border-slate-200 cursor-pointer flex items-center justify-center z-50 rounded-none bg-transparent"
          >
            <span className="material-symbols-outlined text-sm font-bold">close</span>
          </button>
          
          {/* Profile Form */}
          <div className="p-2 md:p-4 text-left">
            <Profile
              initialProfile={currentUser}
              onProfileUpdate={(updated) => {
                setCurrentUser(updated);
                localStorage.setItem('user', JSON.stringify(updated));
              }}
              showToast={showToast}
            />
          </div>
        </div>
      </div>
    )}
    {/* Modal Dialog for Change Password - outside of translating header element */}
    {activeModal === 'changePassword' && (
      <div className="fixed inset-0 bg-black/65 backdrop-blur-sm z-[9999] flex justify-center items-start overflow-y-auto p-4 md:p-8">
        <div className="bg-white rounded-none max-w-xl w-full my-4 md:my-8 shadow-2xl relative border border-outline-variant animate-scale-in">
          {/* Close Button */}
          <button
            onClick={() => setActiveModal(null)}
            className="absolute top-6 right-6 p-2 text-slate-600 hover:text-white hover:bg-primary transition-all border border-slate-200 cursor-pointer flex items-center justify-center z-50 rounded-none bg-transparent"
          >
            <span className="material-symbols-outlined text-sm font-bold">close</span>
          </button>
          
          {/* Change Password Form */}
          <div className="p-2 md:p-4 text-left">
            <ChangePassword
              onCancel={() => setActiveModal(null)}
              showToast={showToast}
            />
          </div>
        </div>
      </div>
    )}

    {/* Modal Dialog for Room Details */}
    {activeModal === 'roomDetail' && (
      <div className="fixed inset-0 bg-black/80 z-[9999] flex justify-center items-center p-4 md:p-8">
        {isLoadingRoomDetail ? (
          <div className="bg-white rounded-none p-12 max-w-md w-full text-center flex flex-col items-center justify-center border border-outline-variant shadow-2xl animate-fade-in">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-primary mb-4"></div>
            <p className="text-xs uppercase font-bold tracking-widest text-slate-500 font-['Montserrat']">{t('nav_loading_room_detail', 'Đang tải thông tin chi tiết phòng...')}</p>
          </div>
        ) : (
          <RoomDetailModern
            roomDetailData={roomDetailData}
            onClose={() => {
              setActiveModal(null);
              setRoomDetailData(null);
            }}
            onBookingPersonal={(room) => {
              setActiveModal(null);
              setIsMobileMenuOpen(false);
              if (localStorage.getItem('accessToken')) {
                sessionStorage.setItem('bookingRoom', JSON.stringify(room));
                handleNavClick('booking');
              } else {
                sessionStorage.setItem('pendingBookingRoom', JSON.stringify(room));
                showToast(t('nav_toast_login_required', 'Vui lòng đăng nhập để tiến hành đặt phòng!'), 'info');
                handleNavClick('login');
              }
            }}
            onBookingGroup={(room) => {
              setActiveModal(null);
              setIsMobileMenuOpen(false);
              if (localStorage.getItem('accessToken')) {
                sessionStorage.setItem('bookingRoom', JSON.stringify(room));
                handleNavClick('group-booking');
              } else {
                sessionStorage.setItem('pendingBookingRoom', JSON.stringify(room));
                showToast(t('nav_toast_login_required_group', 'Vui lòng đăng nhập để tiến hành đặt phòng nhóm!'), 'info');
                handleNavClick('login');
              }
            }}
          />
        )}
      </div>
    )}

    <ToastContainer toasts={toasts} onDismiss={dismissToast} />
  </>
);
}
