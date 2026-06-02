import { useEffect, useState, useRef } from 'react';
import heroElysian from '../assets/hero_elysian.png';

export default function Home({ setActivePage }) {
  const [selectedHotel, setSelectedHotel] = useState('Chọn Khách sạn');
  const [bookingDate, setBookingDate] = useState('02/06/2026 - 03/06/2026');
  const [guests, setGuests] = useState('1 Người lớn, 0 Trẻ em');
  const [isRewardsShrunk, setIsRewardsShrunk] = useState(false);
  const [activeCard, setActiveCard] = useState(null);
  const rewardsRef = useRef(null);

  const benefits = [
    {
      id: 1,
      icon: 'bedtime',
      title: 'SỞ HỮU ĐÊM NGHỈ MIỄN PHÍ',
    },
    {
      id: 2,
      icon: 'groups',
      title: 'GIÁ ƯU ĐÃI DÀNH CHO HỘI VIÊN',
    },
    {
      id: 3,
      icon: 'local_bar',
      title: 'ƯU ĐÃI ẨM THỰC',
    },
    {
      id: 4,
      icon: 'featured_seasonal_and_gifts',
      title: 'NHIỀU ƯU ĐÃI DÀNH RIÊNG',
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
      
      // When the top of the section enters past 75% of viewport height, shrink it
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

  const offers = [
    {
      id: 1,
      image: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800&q=80',
      badge: 'SAVE UP TO 15%',
      title: 'Ưu Đãi 15% Mừng Năm Mới',
      description: 'Khởi đầu năm 2026 trọn vẹn tại Elysian với chương trình ưu đãi đặc biệt giảm giá lên đến 15% khi đặt phòng trực tiếp.',
      highlight: null,
    },
    {
      id: 2,
      image: 'https://images.unsplash.com/photo-1535131749006-b7f58c99034b?w=800&q=80',
      badge: null,
      title: 'Kỳ Nghỉ Golf Trọn Gói (All-in-One)',
      description: 'Gói golf độc quyền mang đến trải nghiệm hoàn hảo, kết nối nhu cầu lưu trú sang trọng, những vòng golf đầy thử thách...',
      highlight: null,
    },
    {
      id: 3,
      image: 'https://images.unsplash.com/photo-1470337458703-46ad1756a187?w=800&q=80',
      badge: null,
      title: 'Tặng Voucher Ăn Uống Cho Mỗi Đêm Lưu Trú',
      description: 'Nhận ngay 100K F&B credit hàng ngày để thoải mái khám phá từ cocktail nghệ thuật đến những món ăn tinh tế tại Elysian Bar.',
      highlight: 'Nhận ngay 100K F&B credit',
    },
  ];

  return (
    <div className="w-full">

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

        <div className="relative z-10 h-full flex flex-col justify-end items-center text-center px-4 pb-60">
          <h1 className="font-bold text-[22px] md:text-[32px] text-white max-w-4xl drop-shadow-lg leading-snug tracking-wider uppercase">
            LƯU TRÚ TRỌN VẸN 24 GIỜ<br />VỚI DỊCH VỤ 24/7
          </h1>
        </div>

        {/* Booking Bar */}
        <div className="absolute bottom-12 left-1/2 -translate-x-1/2 w-full max-w-6xl px-4 z-20">
          <div className="bg-white border border-outline-variant shadow-2xl py-4 px-6 md:py-5 md:px-8 flex flex-col md:flex-row gap-6 items-end text-left">
            <div className="flex-1 w-full space-y-2">
              <label className="block font-bold text-xs text-secondary uppercase">Hotel</label>
              <div className="flex items-center justify-between border-b border-on-surface py-2">
                <select value={selectedHotel} onChange={(e) => setSelectedHotel(e.target.value)}
                  className="w-full bg-transparent border-none focus:ring-0 font-bold text-sm p-0 outline-none cursor-pointer">
                  <option>Chọn Khách sạn</option>
                  <option>Elysian Hotel Saigon Centre</option>
                  <option>Elysian Hotel Danang Centre</option>
                  <option>Elysian Hotel Can Tho</option>
                </select>
                <span className="material-symbols-outlined text-secondary ml-2">location_on</span>
              </div>
            </div>
            <div className="flex-1 w-full space-y-2">
              <label className="block font-bold text-xs text-secondary uppercase">Ngày</label>
              <div className="flex items-center justify-between border-b border-on-surface py-2">
                <input className="w-full bg-transparent border-none focus:ring-0 font-bold text-sm p-0 outline-none"
                  type="text" value={bookingDate} onChange={(e) => setBookingDate(e.target.value)} />
                <span className="material-symbols-outlined text-secondary ml-2">calendar_month</span>
              </div>
            </div>
            <div className="flex-1 w-full space-y-2">
              <label className="block font-bold text-xs text-secondary uppercase">Số Khách</label>
              <div className="flex items-center justify-between border-b border-on-surface py-2">
                <input className="w-full bg-transparent border-none focus:ring-0 font-bold text-sm p-0 outline-none"
                  type="text" value={guests} onChange={(e) => setGuests(e.target.value)} />
                <span className="material-symbols-outlined text-secondary ml-2">group</span>
              </div>
            </div>
            <button className="w-full md:w-auto bg-primary text-on-primary font-bold py-4 px-12 uppercase tracking-widest hover:brightness-110 transition-all cursor-pointer border-none text-xs h-[52px] flex items-center justify-center">
              TÌM PHÒNG
            </button>
          </div>
        </div>
      </section>

      {/* ── TRẢI NGHIỆM ELYSIAN ──────────────────────────────────────────── */}
      <section className="bg-surface-container-low py-20 px-4 md:px-16">
        {/* Heading */}
        <div className="text-center max-w-2xl mx-auto mb-16 scroll-reveal">
          <h2 className="font-bold text-[28px] md:text-[36px] text-on-surface mb-5">Trải nghiệm Elysian</h2>
          <p className="text-secondary text-base leading-relaxed">
            Elysian nổi bật với phong cách thiết kế độc đáo, kết hợp hài hòa giữa tính hiện đại và giá trị
            truyền thống Việt Nam, mang đến trải nghiệm mới mẻ cho du khách.
          </p>
        </div>

        {/* Experience cards container */}
        <div className="max-w-5xl mx-auto">

          {/* Row 1: two parallelogram cards */}
          <div className="flex items-start gap-0 scroll-reveal">

            {/* LEFT column: image only (label below) */}
            <div className="flex-[1.1] flex flex-col">
              {/* Spacer to align image top with right label height */}
              <div className="h-14"></div>
              {/* Left parallelogram image */}
              <div className="overflow-hidden" style={{ clipPath: 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' }}>
                <div className="aspect-[4/3]">
                  <img
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                    alt="Stay24 – guests relaxing in hotel room with a corgi"
                    src="https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&q=80"
                  />
                </div>
              </div>
              {/* STAY24 label – below left image */}
              <div className="mt-4 pl-1">
                <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">STAY24</p>
                <p className="text-secondary text-[10px] uppercase tracking-[0.18em] mt-1.5 font-semibold">NHẬN PHÒNG GIỜ NÀO TRẢ PHÒNG GIỜ ĐÓ</p>
              </div>
            </div>

            {/* RIGHT column: label above, then image */}
            <div className="flex-[1] flex flex-col -ml-10">
              {/* DỊCH VỤ 24/7 label – above right image */}
              <div className="mb-3 pl-10">
                <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">DỊCH VỤ 24/7</p>
                <p className="text-secondary text-[10px] uppercase tracking-[0.15em] mt-1.5 font-semibold leading-snug">
                  ELYSIAN BAR, LAUNDROMAT, GRAB &amp; GO, CO-WORKING SPACE
                </p>
              </div>
              {/* Right parallelogram image */}
              <div className="overflow-hidden" style={{ clipPath: 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' }}>
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
            <div className="w-[52%] overflow-hidden" style={{ clipPath: 'polygon(0% 0%, 88% 0%, 100% 100%, 12% 100%)' }}>
              <div className="aspect-[16/9]">
                <img
                  className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                  alt="Nghỉ ngơi thời công nghệ – modern hotel lobby"
                  src="https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&q=80"
                />
              </div>
            </div>
          </div>

          {/* NGHỈ NGƠI label – below center image */}
          <div className="text-center mt-5 mb-2">
            <p className="font-extrabold text-[15px] uppercase italic text-on-surface tracking-wide leading-none">NGHỈ NGƠI THỜI CÔNG NGHỆ</p>
            <p className="text-secondary text-[10px] uppercase tracking-[0.18em] mt-1.5 font-semibold">NHANH CHÓNG, HIỆU QUẢ &amp; TIỆN DỤNG</p>
          </div>

        </div>
      </section>

      {/* ── ƯU ĐÃI ───────────────────────────────────────────────────────── */}
      <section className="bg-surface-container-low py-20 px-4 md:px-16">
        {/* Heading */}
        <div className="text-center mb-4 scroll-reveal">
          <h2 className="font-bold text-[28px] md:text-[36px] text-on-surface mb-0">Ưu đãi</h2>
        </div>
        <div className="flex flex-col md:flex-row justify-between items-center max-w-5xl mx-auto border-t border-outline-variant pt-5 mb-12">
          <p className="text-secondary text-base text-center md:text-left">
            Khám phá ngay những ưu đãi cực &quot;hot&quot; tại Elysian Hotels!
          </p>
          <div className="flex items-center gap-4 mt-4 md:mt-0">
            <button
              onClick={() => setActivePage('offers')}
              className="flex items-center gap-1 text-xs text-on-surface uppercase tracking-widest hover:text-primary cursor-pointer bg-transparent border-none transition-colors"
            >
              <span className="material-symbols-outlined text-sm">chevron_left</span> TRƯỚC
            </button>
            <span className="text-sm font-bold text-on-surface">3/7</span>
            <button
              onClick={() => setActivePage('offers')}
              className="flex items-center gap-1 text-xs text-on-surface uppercase tracking-widest hover:text-primary cursor-pointer bg-transparent border-none transition-colors"
            >
              TIẾP <span className="material-symbols-outlined text-sm">chevron_right</span>
            </button>
          </div>
        </div>

        {/* 3 Offer Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto scroll-reveal">
          {offers.map((offer) => (
            <div key={offer.id} className="group cursor-pointer" onClick={() => setActivePage('offers')}>
              <div className="relative overflow-hidden aspect-[4/3] bg-surface-container-highest mb-4">
                <img
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                  alt={offer.title}
                  src={offer.image}
                />
                {offer.badge && (
                  <div className="absolute inset-0 bg-black/40 flex items-end p-4">
                    <span className="font-black text-white text-2xl md:text-3xl uppercase leading-tight">
                      {offer.badge}
                    </span>
                  </div>
                )}
              </div>
              <h3 className="font-bold text-base text-on-surface mb-2 group-hover:text-primary transition-colors">
                {offer.title}
              </h3>
              {offer.highlight ? (
                <p className="text-sm text-secondary leading-relaxed">
                  <span className="text-primary font-bold">{offer.highlight}</span>
                  {offer.description.replace(offer.highlight, '')}
                </p>
              ) : (
                <p className="text-sm text-secondary leading-relaxed">{offer.description}</p>
              )}
            </div>
          ))}
        </div>

        {/* XEM TẤT CẢ link */}
        <div className="max-w-5xl mx-auto mt-10">
          <button
            onClick={() => setActivePage('offers')}
            className="flex items-center gap-2 text-primary font-bold uppercase tracking-widest text-sm cursor-pointer bg-transparent border-none p-0 hover:underline"
          >
            <span className="text-primary text-xl font-black">/</span>
            XEM TẤT CẢ
          </button>
        </div>
      </section>

      {/* ── SẮP RA MẮT ───────────────────────────────────────────────────── */}
      <section className="bg-surface-container-low pb-24 px-4 md:px-16">
        <div className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-0 items-center scroll-reveal">
          {/* Left text */}
          <div className="space-y-4 pr-0 md:pr-12 py-12">
            <p className="text-secondary text-xs uppercase tracking-widest font-bold">SẮP RA MẮT</p>
            <h2 className="font-bold text-[28px] md:text-[36px] text-primary leading-tight m-0">
              Elysian Hotel Hanoi
            </h2>
            <p className="text-secondary text-sm leading-relaxed">
              Buzz. Cơn lốc của sự đổi mới đã sẵn sàng tiến đến Thủ đô. Hẹn gặp lại vào năm 2026 ;)
            </p>
          </div>

          {/* Right image with skew + counter */}
          <div className="relative">
            <div className="overflow-hidden aspect-[4/5]" style={{ clipPath: 'polygon(10% 0, 100% 0, 100% 100%, 0 100%)' }}>
              <img
                className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                alt="Elysian Hotel Hanoi architectural rendering"
                src="https://images.unsplash.com/photo-1621847468516-1ed5d0df56fe?w=800&q=80"
              />
            </div>
            {/* Counter badge */}
            <div className="absolute bottom-8 left-0 -translate-x-1/3 bg-white px-6 py-5 shadow-lg border border-outline-variant hidden md:flex items-center justify-center" style={{ clipPath: 'polygon(0 0, 85% 0, 100% 100%, 15% 100%)' }}>
              <span className="font-bold text-base text-on-surface">1/1</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── ELYSIAN REWARDS LIFESTYLE ────────────────────────────────────── */}
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
              Tham gia Elysian Rewards Lifestyle hoàn toàn miễn phí
            </h2>
            <p className="font-body-lg text-body-lg text-secondary italic max-w-2xl mx-auto mt-4">
              "Gia nhập cộng đồng Elysian, không lo về hạng thẻ. Với Elysian, ai cũng là VIP."
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
              ĐĂNG KÝ NGAY
            </button>
          </div>
        </div>
      </section>

    </div>
  );
}
