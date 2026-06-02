import { useState } from 'react';

export default function Offers() {
  const [currentPage, setCurrentPage] = useState(3);
  const totalPages = 7;

  const handlePrev = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
    }
  };

  const offers = [
    {
      id: 1,
      image: "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800&q=80",
      badge: "SAVE UP TO 15%",
      title: "Ưu Đãi 15% Mừng Năm Mới",
      description: "Khởi đầu năm 2026 trọn vẹn tại Elysian với chương trình ưu đãi đặc biệt giảm giá lên đến 15% khi đặt phòng trực tiếp."
    },
    {
      id: 2,
      image: "https://images.unsplash.com/photo-1535131749006-b7f58c99034b?w=800&q=80",
      badge: null,
      title: "Kỳ Nghỉ Golf Trọn Gói (All-in-One)",
      description: "Gói golf độc quyền mang đến trải nghiệm hoàn hảo, kết nối nhu cầu lưu trú sang trọng, những vòng golf đầy thử thách, cùng hệ thống tiện ích đa dạng..."
    },
    {
      id: 3,
      image: "https://images.unsplash.com/photo-1470337458703-46ad1756a187?w=800&q=80",
      badge: null,
      title: "Tặng Voucher Ăn Uống Cho Mỗi Đêm Lưu Trú",
      description: "Nhận ngay 100K F&B credit hàng ngày để thoải mái khám phá từ cocktail nghệ thuật đến những món ăn tinh tế tại Elysian Bar."
    }
  ];

  return (
    <div className="w-full">
      <main className="pt-24 pb-stack-lg text-left">
        {/* Hero Title Section */}
        <header className="px-margin-mobile md:px-margin-desktop mb-stack-lg text-center">
          <h1 className="font-headline-xl text-headline-xl md:text-headline-xl mb-stack-sm text-on-surface">Ưu đãi</h1>
          <div className="flex flex-col md:flex-row justify-between items-center max-w-7xl mx-auto border-t border-outline-variant pt-8">
            <p className="font-body-lg text-body-lg text-secondary max-w-2xl text-center md:text-left">
              Khám phá ngay những ưu đãi cực "hot" tại Elysian Hotels!
            </p>
            <div className="flex items-center gap-6 mt-6 md:mt-0">
              <button 
                onClick={handlePrev}
                disabled={currentPage === 1}
                className={`flex items-center gap-2 text-on-surface bg-transparent border-none ${
                  currentPage === 1 ? 'opacity-30 cursor-not-allowed' : 'hover:text-primary cursor-pointer'
                } transition-colors`}
              >
                <span className="material-symbols-outlined" style={{ fontVariationSettings: "'wght' 700" }}>chevron_left</span>
                <span className="font-label-bold text-label-bold uppercase">TRƯỚC</span>
              </button>
              
              <span className="font-label-bold text-label-bold">{currentPage}/{totalPages}</span>
              
              <button 
                onClick={handleNext}
                disabled={currentPage === totalPages}
                className={`flex items-center gap-2 text-on-surface bg-transparent border-none ${
                  currentPage === totalPages ? 'opacity-30 cursor-not-allowed' : 'hover:text-primary cursor-pointer'
                } transition-colors`}
              >
                <span className="font-label-bold text-label-bold uppercase">TIẾP</span>
                <span className="material-symbols-outlined" style={{ fontVariationSettings: "'wght' 700" }}>chevron_right</span>
              </button>
            </div>
          </div>
        </header>

        {/* Offers Grid */}
        <section className="px-margin-mobile md:px-margin-desktop max-w-[1440px] mx-auto py-8">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-gutter">
            {offers.map((offer) => (
              <article 
                key={offer.id}
                className="group bg-surface-container-lowest border border-transparent hover:border-outline-variant transition-all duration-300 flex flex-col justify-between"
              >
                <div className="relative overflow-hidden aspect-[4/3]">
                  <img 
                    alt={offer.title} 
                    className="w-full h-full object-cover trapezoid-img group-hover:scale-105 transition-transform duration-700" 
                    src={offer.image}
                  />
                  {offer.badge && (
                    <div className="absolute top-8 left-0 bg-primary text-white px-6 py-2 font-headline-lg italic text-sm">
                      {offer.badge}
                    </div>
                  )}
                </div>
                <div className="p-8 flex-grow flex flex-col justify-between">
                  <div>
                    <h2 className="font-headline-md text-headline-md mb-4 text-on-surface group-hover:text-primary transition-colors m-0 text-xl">
                      {offer.title}
                    </h2>
                    <p className="font-body-md text-body-md text-secondary mb-8 line-clamp-3">
                      {offer.description}
                    </p>
                  </div>
                  <a className="inline-flex items-center gap-2 font-label-bold text-label-bold text-on-surface hover:text-primary group/link transition-colors uppercase no-underline font-bold text-sm" href="#">
                    XEM THÊM 
                    <span className="material-symbols-outlined group-hover/link:translate-x-1 transition-transform">arrow_forward</span>
                  </a>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* Secondary CTA Banner */}
        <section className="mt-stack-lg px-margin-mobile md:px-margin-desktop py-8">
          <div className="bg-inverse-surface text-white p-12 relative overflow-hidden flex flex-col md:flex-row items-center justify-between gap-8">
            <div className="absolute right-0 top-0 w-1/2 h-full bg-primary opacity-10 -skew-x-12 translate-x-1/4 pointer-events-none"></div>
            <div className="relative z-10 text-left">
              <h3 className="font-headline-lg text-headline-lg mb-2 m-0">Trở thành Elysian Member ngay!</h3>
              <p className="font-body-lg text-secondary-fixed opacity-80 m-0">
                Nhận thêm 10% giảm giá và nhiều đặc quyền ưu đãi khác chỉ dành cho thành viên.
              </p>
            </div>
            <button className="relative z-10 bg-primary text-white px-12 py-5 font-label-bold uppercase tracking-widest hover:bg-white hover:text-primary transition-all duration-300 cursor-pointer border-none">
              ĐĂNG KÝ MIỄN PHÍ
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
