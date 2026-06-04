import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Profile from '../pages/Profile';
import ChangePassword from '../pages/ChangePassword';
import { useToast, ToastContainer } from './Toast';
import { getUserProfile } from '../services/userService';

export default function Navbar({ activePage, setActivePage }) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [menuState, setMenuState] = useState('idle'); // 'idle' | 'open' | 'closed'
  const [isScrolled, setIsScrolled] = useState(false);
  const [lang, setLang] = useState('VN');
  const [hoveredId, setHoveredId] = useState(null);
  const [underlineStyle, setUnderlineStyle] = useState({ left: 0, width: 0, opacity: 0 });
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [activeModal, setActiveModal] = useState(null); // 'profile' | null

  const navContainerRef = useRef(null);
  const dropdownRef = useRef(null);
  const itemRefs = useRef({});
  const navigate = useNavigate();
  const { toasts, showToast, dismissToast } = useToast();

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
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

  const navItems = [
    { id: 'home', label: 'KHÁCH SẠN' },
    { id: 'residences', label: 'ELYSIAN RESIDENCES' },
    { id: 'experiences', label: 'TRẢI NGHIỆM ELYSIAN' },
    { id: 'events', label: 'HỘI NGHỊ & SỰ KIỆN' },
    { id: 'offers', label: 'ƯU ĐÃI' }
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
        {/* Language selector on the right (flag and arrow dropdown, no text) */}
        <div className="flex-1 flex justify-end">
          <button 
            onClick={() => setLang(lang === 'VN' ? 'EN' : 'VN')}
            className="flex items-center gap-1 text-white hover:text-primary cursor-pointer bg-transparent border-none"
          >
            <span className="w-5 h-3.5 flex items-center overflow-hidden border border-white/20">
              {lang === 'VN' ? (
                <span className="w-full h-full bg-[#da251d] relative flex items-center justify-center">
                  <span className="text-[8px] text-[#ffff00] leading-none">★</span>
                </span>
              ) : (
                <span className="w-full h-full bg-[#0a3161] relative flex flex-wrap">
                  <span className="w-1/2 h-full bg-[#0a3161] text-[6px] text-white flex items-center justify-center leading-none">*</span>
                  <span className="w-1/2 h-full bg-white flex flex-col">
                    <span className="h-1/3 bg-[#b31942]"></span>
                    <span className="h-1/3 bg-white"></span>
                    <span className="h-1/3 bg-[#b31942]"></span>
                  </span>
                </span>
              )}
            </span>
            <span className="material-symbols-outlined text-sm leading-none opacity-80">arrow_drop_down</span>
          </button>
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
        <div className="h-full flex items-center pr-6 relative">
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
                      setActiveModal('profile');
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
                        setActiveModal('changePassword');
                        setIsDropdownOpen(false);
                      }}
                      className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-[10px] font-bold uppercase tracking-widest text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors cursor-pointer border-none bg-transparent rounded-none"
                    >
                      <span className="material-symbols-outlined text-base">lock</span>
                      <span>Đổi mật khẩu</span>
                    </button>

                    <div className="h-px bg-slate-100 my-2" />

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
              ĐẶT NGAY
            </button>
          )}
        </div>
      </div>

      {/* Mobile Drawer Navigation */}
      {isMobileMenuOpen && (
        <div className="absolute top-24 left-0 right-0 bg-surface border-b border-outline-variant shadow-xl md:hidden z-40 animate-fade-in">
          <div className="flex flex-col p-4 space-y-4 text-left">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => handleNavClick(item.id)}
                className={`text-left font-label-bold text-xs uppercase tracking-wider py-3 border-b border-outline-variant/30 cursor-pointer bg-transparent ${
                  activePage === item.id ? 'text-primary font-bold' : 'text-on-surface'
                }`}
              >
                {item.label}
              </button>
            ))}
            <div className="flex items-center justify-between py-2">
              <span className="text-xs text-secondary font-bold">NGÔN NGỮ / LANGUAGE</span>
              <button 
                onClick={() => setLang(lang === 'VN' ? 'EN' : 'VN')}
                className="flex items-center gap-2 text-on-surface bg-transparent border-none cursor-pointer"
              >
                <span className="material-symbols-outlined">language</span>
                <span className="text-xs font-bold">{lang}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </header>

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

    <ToastContainer toasts={toasts} onDismiss={dismissToast} />
  </>
);
}
