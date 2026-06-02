import { useEffect, useState } from 'react';
import Rewards from './Rewards';

export default function Experiences({ setActivePage }) {
  const [subTab, setSubTab] = useState('services'); // 'services' or 'rewards'

  useEffect(() => {
    const observerOptions = {
      threshold: 0.1
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('opacity-100', 'translate-y-0');
          entry.target.classList.remove('opacity-0', 'translate-y-10');
        }
      });
    }, observerOptions);

    document.querySelectorAll('.animate-on-scroll').forEach(section => {
      section.classList.add('transition-all', 'duration-1000', 'opacity-0', 'translate-y-10');
      observer.observe(section);
    });

    return () => {
      observer.disconnect();
    };
  }, [subTab]); // re-run observer when subTab changes

  return (
    <div className="w-full">
      {/* Sub navigation tabs */}
      <div className="bg-surface-container border-b border-outline-variant pt-24 pb-2 flex justify-center gap-6">
        <button
          onClick={() => setSubTab('services')}
          className={`font-label-bold text-label-bold uppercase tracking-wider py-2 px-4 cursor-pointer bg-transparent border-none ${
            subTab === 'services'
              ? 'text-primary border-b-2 border-primary font-bold'
              : 'text-on-surface hover:text-primary border-b-2 border-transparent'
          }`}
        >
          Công Nghệ & Dịch Vụ
        </button>
        <button
          onClick={() => setSubTab('rewards')}
          className={`font-label-bold text-label-bold uppercase tracking-wider py-2 px-4 cursor-pointer bg-transparent border-none ${
            subTab === 'rewards'
              ? 'text-primary border-b-2 border-primary font-bold'
              : 'text-on-surface hover:text-primary border-b-2 border-transparent'
          }`}
        >
          Hội Viên Elysian Rewards
        </button>
      </div>

      {subTab === 'services' ? (
        <div>
          {/* Hero / Section 1: Intro */}
          <section className="relative bg-surface-container-low py-stack-lg overflow-hidden text-left">
            <div className="max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop grid grid-cols-1 md:grid-cols-12 gap-gutter items-center animate-on-scroll">
              <div className="md:col-span-5 z-10">
                <h1 className="font-headline-xl text-headline-xl uppercase italic mb-stack-sm leading-tight">
                  NGHỈ NGƠI THỜI <br /> <span className="text-primary">CÔNG NGHỆ</span>
                </h1>
                <p className="font-body-lg text-body-lg text-secondary italic uppercase tracking-widest">
                  NHANH CHÓNG, HIỆU QUẢ & TIỆN DỤNG
                </p>
                <div className="w-24 h-1 bg-primary mt-8"></div>
              </div>
              <div className="md:col-span-7 relative">
                <div className="elysian-parallelogram overflow-hidden shadow-2xl transition-transform duration-500 hover:scale-[1.02]">
                  <img 
                    alt="Elysian Experience Lobby" 
                    className="w-full h-[300px] md:h-[500px] object-cover" 
                    src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80"
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Section 2: Services Split Layout */}
          <section className="py-stack-lg bg-surface text-left">
            <div className="max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop animate-on-scroll">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-stack-lg items-center mb-stack-lg">
                {/* Stay24 Left Content */}
                <div className="order-2 md:order-1">
                  <div className="elysian-trapezoid-right overflow-hidden mb-stack-md group">
                    <img 
                      alt="Stay24 Service" 
                      className="w-full h-[300px] md:h-[400px] object-cover transition-transform duration-700 group-hover:scale-110" 
                      src="https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80"
                    />
                  </div>
                  <h2 className="font-headline-md text-headline-md uppercase italic">STAY24</h2>
                  <p className="font-body-md text-body-md text-secondary italic">NHẬN PHÒNG GIỜ NÀO TRẢ PHÒNG GIỜ ĐÓ</p>
                </div>
                {/* Grab & Go Right Content */}
                <div className="order-1 md:order-2 text-right">
                  <h2 className="font-headline-md text-headline-md uppercase italic">DỊCH VỤ 24/7</h2>
                  <p className="font-body-md text-body-md text-secondary italic mb-stack-md max-w-sm ml-auto">
                    ELYSIAN BAR, LAUNDROMAT, GRAB & GO, CO-WORKING SPACE
                  </p>
                  <div className="elysian-trapezoid-left overflow-hidden group">
                    <img 
                      alt="Grab and Go Shop" 
                      className="w-full h-[300px] md:h-[400px] object-cover transition-transform duration-700 group-hover:scale-110" 
                      src="https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&q=80"
                    />
                  </div>
                </div>
              </div>

              {/* Section 3: Bento Experience Grid */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter mt-stack-lg">
                <div className="bg-surface-container p-stack-md border-l-4 border-primary">
                  <span className="material-symbols-outlined text-primary text-4xl mb-4">speed</span>
                  <h3 className="font-headline-md text-headline-md mb-2">CHECK-IN SIÊU TỐC</h3>
                  <p className="font-body-md text-body-md text-tertiary">
                    Bỏ qua mọi thủ tục rườm rà. Chỉ vài thao tác trên ứng dụng di động để mở cửa phòng ngay lập tức.
                  </p>
                </div>
                <div className="bg-inverse-surface p-stack-md text-white">
                  <span className="material-symbols-outlined text-primary-fixed text-4xl mb-4">hub</span>
                  <h3 className="font-headline-md text-headline-md mb-2 text-white">HUB KẾT NỐI</h3>
                  <p className="font-body-md text-body-md text-secondary-fixed">
                    Không chỉ là sảnh chờ, đây là không gian làm việc sáng tạo và gặp gỡ cộng đồng bản địa năng động.
                  </p>
                </div>
                <div className="bg-surface-container p-stack-md border-r-4 border-primary text-right">
                  <span className="material-symbols-outlined text-primary text-4xl mb-4">local_bar</span>
                  <h3 className="font-headline-md text-headline-md mb-2">ELYSIAN BAR</h3>
                  <p className="font-body-md text-body-md text-tertiary">
                    Thưởng thức craft beer địa phương và cocktail đặc trưng trong không gian âm nhạc hiện đại suốt đêm.
                  </p>
                </div>
              </div>
            </div>
          </section>

          {/* CTA Section */}
          <section className="relative h-[400px] flex items-center justify-center overflow-hidden">
            <div className="absolute inset-0 bg-black/40 z-10"></div>
            <img 
              alt="Elysian Rooftop" 
              className="absolute inset-0 w-full h-full object-cover" 
              src="https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&q=80"
            />
            <div className="relative z-20 text-center px-4">
              <h2 className="font-headline-xl text-headline-xl text-white uppercase italic mb-stack-md">SẴN SÀNG TRẢI NGHIỆM?</h2>
              <button 
                onClick={() => setActivePage('offers')}
                className="bg-primary hover:bg-surface-tint text-white px-12 py-5 font-label-bold text-label-bold uppercase tracking-[0.2em] transition-all duration-300 transform hover:skew-x-[-12deg] cursor-pointer border-none"
              >
                KHÁM PHÁ NGAY
              </button>
            </div>
          </section>
        </div>
      ) : (
        <Rewards setActivePage={setActivePage} />
      )}
    </div>
  );
}
