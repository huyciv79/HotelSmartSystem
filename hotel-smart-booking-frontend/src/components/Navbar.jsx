import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import hotelLogo from '../assets/hotel_logo.png';
import Profile from '../pages/Profile';
import ChangePassword from '../pages/ChangePassword';
import { useToast, ToastContainer } from './Toast';
import NotificationDropdown from './NotificationDropdown';
import { getUserProfile } from '../services/userService';
import { getRoomTypes, getRoomTypeDetail } from '../services/roomService';
import RoomDetailModern from './room/RoomDetailModern';
import { useLanguage } from '../context/LanguageContext';

const defaultRoomTypes = [
  { id: 17, name: 'Standard No Window', basePrice: 850000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Standard%20No%20Window/Standard%20No%20Window%20.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvU3RhbmRhcmQgTm8gV2luZG93L1N0YW5kYXJkIE5vIFdpbmRvdyAuanBnIiwiaWF0IjoxNzgwNDY1MjkwLCJleHAiOjE4MTIwMDEyOTB9.4neMHzSAVG90PHsmwr1_nreyjB9L5RzTEgChopDYmZU' },
  { id: 22, name: 'Standard Double City View', basePrice: 1050000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Standard%20Double%20City%20View/Standard%20Double%20City%20View1.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvU3RhbmRhcmQgRG91YmxlIENpdHkgVmlldy9TdGFuZGFyZCBEb3VibGUgQ2l0eSBWaWV3MS5qcGciLCJpYXQiOjE3ODA0Njc1MDUsImV4cCI6MTgxMjAwMzUwNX0.3R-rkQnv9o3CISiotUnXnbEhZTjVzg8KM2Jgf1tKRc4' },
  { id: 18, name: 'Premium Double with City View', basePrice: 1350000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Premium%20Double%20with%20City%20View/PremiumDoublewithCityView01.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvUHJlbWl1bSBEb3VibGUgd2l0aCBDaXR5IFZpZXcvUHJlbWl1bURvdWJsZXdpdGhDaXR5VmlldzAxLmpwZyIsImlhdCI6MTc4MDQ2NTc1MSwiZXhwIjoxODEyMDAxNzUxfQ.6M5udbivH5nARLnCG7qyXIix_8bCucNXIWMRLrSpA6k' },
  { id: 19, name: 'Executive Double with River View', basePrice: 1650000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Executive%20Double%20with%20River%20View/Executive%20Double%20with%20River%20View1.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvRXhlY3V0aXZlIERvdWJsZSB3aXRoIFJpdmVyIFZpZXcvRXhlY3V0aXZlIERvdWJsZSB3aXRoIFJpdmVyIFZpZXcxLmpwZyIsImlhdCI6MTc4MDQ2NjQ4OSwiZXhwIjoxODEyMDAyNDg5fQ.dIq1CS91-j72dc3wKMmZ2pIv0fsYz6s3XRwB9-1-Ozg' },
  { id: 23, name: 'Premium Triple Room City View', basePrice: 1950000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Premium%20Triple%20Room%20City%20View/Premium%20Triple%20Room%20City%20View2.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvUHJlbWl1bSBUcmlwbGUgUm9vbSBDaXR5IFZpZXcvUHJlbWl1bSBUcmlwbGUgUm9vbSBDaXR5IFZpZXcyLmpwZyIsImlhdCI6MTc4MDQ2NTkxNiwiZXhwIjoxODEyMDAxOTE2fQ.HV2cX-JB5_wdm9oahpM5_UYqrkl2k0EVuS01uM0c5XY' },
  { id: 20, name: 'Deluxe River View with Balcony', basePrice: 2250000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Deluxe%20River%20View%20with%20Balcony/Deluxe%20River%20View%20with%20Balcony1.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvRGVsdXhlIFJpdmVyIFZpZXcgd2l0aCBCYWxjb255L0RlbHV4ZSBSaXZlciBWaWV3IHdpdGggQmFsY29ueTEuanBnIiwiaWF0IjoxNzgwNDY2Njg6LCJleHAiOjE4MTIwMDI2ODZ9.7v2w3hKuDe_lxygX-i3kHWYIIjrRp6Rl1AQezgVTERc' },
  { id: 21, name: 'Suite River View', basePrice: 2850000, primaryImageUrl: 'https://oblnnzyndmubiednvaqf.supabase.co/storage/v1/object/sign/roomtype-images/Suite%20River%20View/SuiteRiverView01.avif?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV85MDUyMzRkMy01MDY3LTQ3NDgtOWViYS0xOThkOWIyZjE4NWUiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJyb29tdHlwZS1pbWFnZXMvU3VpdGUgUml2ZXIgVmlldy9TdWl0ZVJpdmVyVmlldzAxLmF2aWYiLCJpYXQiOjE3ODA0Njc4NzYsImV4cCI6MTgxMjAwMjg3Nn0.izuI7mdcYzSrEE_7biJlc5vBZgew70A7rb5PZadFBF8' }
];

export default function Navbar({ activePage, setActivePage, isMobileMenuOpen, setIsMobileMenuOpen }) {
  const { language, setLanguage, t } = useLanguage();
  const [menuState, setMenuState] = useState('idle'); // 'idle' | 'open' | 'closed'
  const [isScrolled, setIsScrolled] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [activeModal, setActiveModal] = useState(null); // 'profile' | null
  const [roomTypes, setRoomTypes] = useState(defaultRoomTypes);
  const [showRoomTypes, setShowRoomTypes] = useState(false);
  const [selectedRoom, setSelectedRoom] = useState(defaultRoomTypes[0]);
  const [prevRoom, setPrevRoom] = useState(null);
  const [slideDirection, setSlideDirection] = useState('right'); // 'left' | 'right'
  const [roomDetailData, setRoomDetailData] = useState(null);
  const [isLoadingRoomDetail, setIsLoadingRoomDetail] = useState(false);
 
  const navContainerRef = useRef(null);
  const dropdownRef = useRef(null);
  const langDropdownRef = useRef(null);
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
  const fetchRooms = async () => {
    try {
      const response = await getRoomTypes();
      let roomsList = [];
      if (response?.data?.content) {
        roomsList = response.data.content;
      } else if (Array.isArray(response?.data)) {
        roomsList = response.data;
      } else if (Array.isArray(response)) {
        roomsList = response;
      }
      if (roomsList.length > 0) {
        setRoomTypes(roomsList);
        setSelectedRoom(roomsList[0]);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách loại phòng:', err);
    }
  };

  useEffect(() => {
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
    const targetId = roomId || selectedRoom?.id || defaultRoomTypes[0].id;
    const initialData = roomTypes.find(r => r.id === targetId) || selectedRoom || defaultRoomTypes[0];
    setRoomDetailData(initialData);
    setActiveModal('roomDetail');
    setIsLoadingRoomDetail(false);

    try {
      const response = await getRoomTypeDetail(targetId);
      const detailData = response?.data || response;
      if (detailData && detailData.name) {
        setRoomDetailData(detailData);
      }
    } catch (err) {
      console.warn('Sử dụng thông tin phòng có sẵn:', err);
    }
  };

  const navItems = [
    { id: 'home', label: t('nav_hotels') },
    { id: 'residences', label: t('nav_residences') },
    { id: 'experiences', label: t('nav_experiences') },
    { id: 'terms-of-service', label: t('footer_terms', 'ĐIỀU KHOẢN').toUpperCase() }
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
        <div className="flex-1 flex justify-center items-center flex-col">
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
        <nav ref={navContainerRef} className="hidden md:flex gap-6 absolute left-1/2 -translate-x-1/2 justify-center items-center h-full">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => handleNavClick(item.id)}
              className={`uppercase transition-colors duration-300 h-full cursor-pointer bg-transparent border-none font-bold text-[10.5px] tracking-tight flex items-center relative after:content-[''] after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-primary after:scale-x-0 hover:after:scale-x-100 after:transition-transform after:duration-200 ${
                activePage === item.id 
                  ? 'text-primary after:scale-x-100' 
                  : 'text-on-surface hover:text-primary'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>

        {/* Book Now / Profile Dropdown */}
        <div className="h-full flex items-center pr-6 relative gap-2">
          {isLoggedIn && (
            <NotificationDropdown dark={false} onNewNotification={(n) => {
              showToast(`${t(n.title, n.title)}: ${t(n.message, n.message)}`, 'info');
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
                    {t('nav_view_profile', 'Xem tất cả trang cá nhân')}
                  </button>
 
                  <div className="h-px bg-slate-200 my-3" />
 
                  {/* Options */}
                  <div className="space-y-1">
                    <button
                      onClick={() => {
                        setActiveModal('changePassword');
                        setIsDropdownOpen(false);
                      }}
                      className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-[10px] font-bold uppercase tracking-widest text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer border-none bg-transparent rounded-none"
                    >
                      <span className="material-symbols-outlined text-base">vpn_key</span>
                      <span>{t('Đổi mật khẩu')}</span>
                    </button>

                    <button
                      onClick={() => {
                        showToast(t('Đăng xuất thành công!'), 'success');
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
                      <span>{t('nav_logout', 'Đăng xuất')}</span>
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
    <div className={`fixed left-0 right-0 bottom-0 bg-[#0B192C] md:bg-background z-10 overflow-y-auto drawer-menu ${
      isScrolled ? 'top-12' : 'top-24'
    } ${
      isMobileMenuOpen ? 'open' : ''
    }`}>
        <div className="w-full grid grid-cols-1 md:grid-cols-12 gap-0 min-h-full items-stretch">
          
          {/* Left Column: Menu Items & Secondary Links (col-span-4) */}
          <div className="md:col-span-4 flex flex-col justify-start min-h-[300px] md:min-h-[450px] py-8 md:py-12 px-margin-mobile md:pl-margin-desktop md:pr-12 self-stretch bg-[#0B192C] md:bg-background">
            {/* Main Navigation List */}
            <div className="flex flex-col space-y-7 text-left font-['Montserrat'] select-none">

              {/* LOẠI PHÒNG (Triggers automatic Room Detail Popup) */}
              <div className="flex flex-col text-left">
                <button
                  onClick={() => {
                    setIsMobileMenuOpen(false);
                    const targetRoomId = selectedRoom?.id || (roomTypes.length > 0 ? roomTypes[0].id : 17);
                    handleOpenRoomDetail(targetRoomId);
                  }}
                  className="group text-left font-semibold text-[17px] md:text-[22px] uppercase tracking-widest cursor-pointer bg-transparent border-none w-fit transition-all duration-300"
                >
                  <span className="relative pb-1 transition-colors duration-300 font-semibold text-white md:text-slate-800 hover:text-primary">
                    {t('nav_room_types')}
                    <span className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-primary transition-transform duration-300 origin-left scale-x-0 group-hover:scale-x-100" />
                  </span>
                </button>
              </div>

              {/* Other items */}
              {navItems.slice(1).map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    setShowRoomTypes(false);
                    handleNavClick(item.id);
                  }}
                  className="group text-left font-semibold text-[17px] md:text-[22px] uppercase tracking-widest cursor-pointer bg-transparent border-none w-fit transition-all duration-300"
                >
                  <span className={`relative pb-1 transition-colors duration-300 font-semibold ${
                    (activePage === item.id && !showRoomTypes) ? 'text-primary' : 'text-white md:text-slate-800 group-hover:text-primary'
                  }`}>
                    {item.label}
                    <span className={`absolute bottom-0 left-0 right-0 h-[2.5px] bg-primary transition-transform duration-300 origin-left ${
                      (activePage === item.id && !showRoomTypes) ? 'scale-x-100' : 'scale-x-0 group-hover:scale-x-100'
                    }`} />
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* Right Column: Dynamic Room Image Carousel (col-span-8) */}
          <div className="md:col-span-8 w-full relative min-h-[400px] md:min-h-full self-stretch overflow-hidden bg-slate-950">
            <div className="w-full h-full relative group">
              
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
                  <span className="text-[10px] font-semibold text-white/80 uppercase tracking-widest block mb-1">
                    {selectedRoom?.bedType || 'ELYSIAN HOTELS & RESORTS'}
                  </span>
                  <h3 className="text-2xl font-semibold text-white uppercase tracking-wider leading-tight">
                    {selectedRoom?.name || 'KỲ NGHỈ DƯỠNG THƯỢNG LƯU'}
                  </h3>
                  {selectedRoom && (
                    <p className="text-xs font-semibold text-white/95 uppercase tracking-widest mt-1.5">
                      {selectedRoom?.basePrice != null && !isNaN(selectedRoom.basePrice) ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedRoom.basePrice) : '1.350.000 ₫'} / ĐÊM
                    </p>
                  )}
                </div>

                {/* Chi tiết Button (Bottom Right) */}
                {selectedRoom && (
                  <div className="absolute bottom-8 right-28 z-10 animate-room-btn">
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
            onPrevRoomType={() => {
              if (!roomTypes || roomTypes.length === 0) return;
              const currIdx = roomTypes.findIndex(r => r.id === roomDetailData?.id);
              const prevIdx = currIdx <= 0 ? roomTypes.length - 1 : currIdx - 1;
              handleOpenRoomDetail(roomTypes[prevIdx].id);
            }}
            onNextRoomType={() => {
              if (!roomTypes || roomTypes.length === 0) return;
              const currIdx = roomTypes.findIndex(r => r.id === roomDetailData?.id);
              const nextIdx = currIdx === -1 || currIdx >= roomTypes.length - 1 ? 0 : currIdx + 1;
              handleOpenRoomDetail(roomTypes[nextIdx].id);
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
