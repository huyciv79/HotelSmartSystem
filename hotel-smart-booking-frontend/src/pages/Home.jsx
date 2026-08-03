import { useEffect, useState, useRef } from 'react';
import heroElysian from '../assets/hero_elysian.png';
import { useLanguage } from '../context/LanguageContext';

export default function Home({ setActivePage }) {
  const { t } = useLanguage();
  const [selectedHotel, setSelectedHotel] = useState('Elysian Hotel Can Tho');
  const [bookingDate, setBookingDate] = useState('02/06/2026 - 03/06/2026');
  const [guests, setGuests] = useState('1 Người lớn, 0 Trẻ em');
  const [isRewardsShrunk, setIsRewardsShrunk] = useState(false);
  const [activeCard, setActiveCard] = useState(null);
  const rewardsRef = useRef(null);

  const benefits = [
    {
      id: 1,
      icon: 'bedtime',
      title: t('rewards_benefit1'),
    },
    {
      id: 2,
      icon: 'groups',
      title: t('rewards_benefit2'),
    },
    {
      id: 3,
      icon: 'local_bar',
      title: t('rewards_benefit3'),
    },
    {
      id: 4,
      icon: 'featured_seasonal_and_gifts',
      title: t('rewards_benefit4'),
    }
  ];

  useEffect(() => {
    const observerOptions = { threshold: 0.1 };
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) entry.target.classList.add('active');
      });
    }, observerOptions);
    document.querySelectorAll('.scroll-reveal').forEach(el => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      if (!rewardsRef.current) return;
      const rect = rewardsRef.current.getBoundingClientRect();
      const windowHeight = window.innerHeight;
      
      if (rect.top < windowHeight * 0.75) {
        setIsRewardsShrunk(true);
      } else {
        setIsRewardsShrunk(false);
      }
    };

    window.addEventListener('scroll', handleScroll);
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);



  return (
    <div className="w-full text-left">

      {/* ── HERO SECTION ─────────────────────────────────────────────────── */}
      <section className="relative h-[680px] w-full mt-24">
        <div className="absolute inset-0 z-0 overflow-hidden">
          <img
            className="w-full h-full object-cover"
            alt="A modern lifestyle photograph of a digital nomad floating in a bright pool"
            src={heroElysian}
          />
          <div className="absolute inset-0 bg-black/20"></div>
        </div>

        <div className="relative z-10 h-full flex flex-col justify-end items-center text-center px-4 pb-32">
          <h1 className="font-bold text-[22px] md:text-[32px] text-white max-w-4xl drop-shadow-lg leading-snug tracking-wider uppercase whitespace-pre-line">
            {t('hero_title')}
          </h1>
        </div>
      </section>

      {/* ── TRẢI NGHIỆM ELYSIAN ──────────────────────────────────────────── */}
      <section className="bg-surface-container-low py-20 px-4 md:px-16">
        {/* Heading */}
        <div className="text-center max-w-2xl mx-auto mb-16 scroll-reveal">
          <h2 className="font-bold text-[28px] md:text-[36px] text-on-surface mb-5">{t('exp_title')}</h2>
          <p className="text-secondary text-base leading-relaxed">
            {t('exp_desc')}
          </p>
        </div>

        {/* Experience cards container */}
        <div className="max-w-5xl mx-auto">

          {/* Row 1: two cards, stacked on mobile, parallelograms on desktop */}
          <div className="flex flex-col md:flex-row items-center md:items-start gap-6 md:gap-0 scroll-reveal">

            {/* LEFT column */}
            <div className="w-full md:flex-[1.1] flex flex-col">
              {/* Spacer to align image top with right label height on desktop */}
              <div className="hidden md:block h-14"></div>
              {/* Left image */}
              <div className="overflow-hidden rounded-xl md:rounded-none" style={{ clipPath: window.innerWidth > 768 ? 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' : 'none' }}>
                <div className="aspect-[4/3]">
                  <img
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                    alt="Stay24 – guests relaxing in hotel room with a corgi"
                    src="https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&q=80"
                  />
                </div>
              </div>
              {/* STAY24 label */}
              <div className="mt-4 px-2 md:pl-1 text-center md:text-left">
                <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">STAY24</p>
                <p className="text-secondary text-[11px] md:text-[10px] uppercase tracking-[0.18em] mt-1.5 font-semibold">{t('exp_stay24_desc')}</p>
              </div>
            </div>

            {/* RIGHT column */}
            <div className="w-full md:flex-[1] flex flex-col md:-ml-10">
              {/* DỊCH VỤ 24/7 label */}
              <div className="mb-3 px-2 md:pl-10 text-center md:text-left">
                <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">{t('exp_service247_desc').split(',')[0]}</p>
                <p className="text-secondary text-[11px] md:text-[10px] uppercase tracking-[0.15em] mt-1.5 font-semibold leading-snug">
                  {t('exp_service247_desc')}
                </p>
              </div>
              {/* Right image */}
              <div className="overflow-hidden rounded-xl md:rounded-none" style={{ clipPath: window.innerWidth > 768 ? 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' : 'none' }}>
                <div className="aspect-[4/3]">
                  <img
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                    alt="Dịch vụ 24/7 – vibrant grab & go store"
                    src="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&q=80"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Row 2: single centered card */}
          <div className="flex justify-center mt-8 scroll-reveal">
            <div className="w-full md:w-[52%] overflow-hidden rounded-xl md:rounded-none" style={{ clipPath: window.innerWidth > 768 ? 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' : 'none' }}>
              <div className="aspect-[16/9]">
                <img
                  className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                  alt="Nghỉ ngơi thời công nghệ – modern hotel lobby"
                  src="https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&q=80"
                />
              </div>
            </div>
          </div>

          {/* NGHỈ NGƠI label */}
          <div className="text-center mt-5 mb-2 px-2">
            <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">{t('exp_tech')}</p>
            <p className="text-secondary text-[11px] md:text-[10px] uppercase tracking-[0.18em] mt-1.5 font-semibold">{t('exp_tech_desc')}</p>
          </div>

        </div>
      </section>





      <section 
        ref={rewardsRef} 
        className={`elysian-pattern relative transition-all duration-1000 ease-out flex items-center justify-center overflow-hidden w-full ${
          isRewardsShrunk ? 'py-16 md:py-24 px-4 md:px-16' : 'py-0 px-0'
        }`}
      >
        <div 
          className={`bg-white w-full transition-all duration-1000 ease-out z-10 text-center flex flex-col justify-center border-outline-variant ${
            isRewardsShrunk 
              ? 'max-w-6xl p-8 md:p-16 border shadow-2xl' 
              : 'max-w-full min-h-[600px] p-16 md:p-24 border-0 shadow-none'
          }`}
        >
          <div className="max-w-4xl mx-auto text-center space-y-4">
            <h2 className="font-headline-xl text-headline-xl text-primary leading-tight m-0 text-[28px] md:text-[36px] font-bold">
              {t('rewards_title')}
            </h2>
            <p className="font-body-lg text-body-lg text-secondary italic max-w-2xl mx-auto mt-4">
              {t('rewards_quote')}
            </p>
          </div>

          {/* Benefits Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mt-12 max-w-5xl mx-auto w-full">
            {benefits.map((b) => (
              <div 
                key={b.id}
                onMouseEnter={() => setActiveCard(b.id)}
                onMouseLeave={() => setActiveCard(null)}
                className="border border-outline-variant p-8 flex flex-col items-center justify-center text-center gap-4 hover:bg-surface-container-low transition-colors duration-300 group cursor-pointer"
              >
                <div className={`w-16 h-16 flex items-center justify-center transition-colors duration-300 ${
                  activeCard === b.id ? 'bg-primary' : 'bg-surface-variant'
                }`}>
                  <span 
                    className={`material-symbols-outlined text-4xl transition-all ${
                      activeCard === b.id ? 'text-on-primary' : 'text-on-surface'
                    }`}
                    style={{
                      fontVariationSettings: activeCard === b.id ? "'FILL' 1" : "'FILL' 0"
                    }}
                  >
                    {b.icon}
                  </span>
                </div>
                <h3 className="font-label-bold text-label-bold uppercase tracking-widest text-on-surface text-sm">{b.title}</h3>
              </div>
            ))}
          </div>

          <div className="mt-12 text-center">
            <button 
              onClick={() => setActivePage('register')}
              className="bg-primary text-on-primary px-12 py-5 font-label-bold uppercase tracking-widest hover:scale-105 transition-all duration-300 cursor-pointer border-none"
            >
              {t('rewards_btn_register')}
            </button>
          </div>
        </div>
      </section>

    </div>
  );
}
