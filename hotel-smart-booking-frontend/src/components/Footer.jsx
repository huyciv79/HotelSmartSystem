export default function Footer({ setActivePage }) {
  const handleNavClick = (id) => {
    setActivePage(id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="bg-inverse-surface text-on-primary-container py-stack-lg px-4 md:px-margin-desktop mt-auto">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-12 gap-gutter">
        <div className="md:col-span-4 space-y-8 text-left">
          <h2 className="font-headline-md text-headline-md text-white tracking-tighter">ELYSIAN</h2>
          <p className="font-body-md text-body-md text-on-tertiary-container max-w-sm">
            Khách sạn Elysian thay đổi định nghĩa về sự sang trọng hiện đại tại Việt Nam. Kết nối bạn với tinh hoa của mỗi thành phố.
          </p>
          <div className="flex gap-4">
            <a className="w-10 h-10 border border-on-tertiary-container flex items-center justify-center text-white hover:bg-primary transition-colors cursor-pointer" href="#" aria-label="Share">
              <span className="material-symbols-outlined">share</span>
            </a>
            <a className="w-10 h-10 border border-on-tertiary-container flex items-center justify-center text-white hover:bg-primary transition-colors cursor-pointer" href="mailto:info@elysian-hotels.com" aria-label="Mail">
              <span className="material-symbols-outlined">mail</span>
            </a>
          </div>
        </div>

        <div className="md:col-span-8 grid grid-cols-2 md:grid-cols-3 gap-8 text-left">
          <div className="space-y-4">
            <p className="font-label-bold text-label-bold text-white uppercase">Công Ty</p>
            <ul className="space-y-2 list-none p-0">
              <li>
                <button onClick={() => handleNavClick('home')} className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors bg-transparent border-none p-0 cursor-pointer">
                  VỀ ELYSIAN HOTELS
                </button>
              </li>
              <li>
                <a className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors no-underline block" href="#">
                  NGHỀ NGHIỆP
                </a>
              </li>
              <li>
                <a className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors no-underline block" href="#">
                  ELYSIAN + MÔI TRƯỜNG
                </a>
              </li>
            </ul>
          </div>
          <div className="space-y-4">
            <p className="font-label-bold text-label-bold text-white uppercase">Khám Phá</p>
            <ul className="space-y-2 list-none p-0">
              <li>
                <button onClick={() => handleNavClick('offers')} className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors bg-transparent border-none p-0 cursor-pointer">
                  ƯU ĐÃI
                </button>
              </li>
              <li>
                <a className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors no-underline block" href="#">
                  BLOGS
                </a>
              </li>
              <li>
                <a className="font-label-bold text-label-bold text-on-tertiary-container hover:text-primary-fixed transition-colors no-underline block" href="#">
                  LIÊN HỆ
                </a>
              </li>
            </ul>
          </div>
          <div className="col-span-2 md:col-span-1 space-y-4">
            <p className="font-label-bold text-label-bold text-white uppercase">Bản Tin</p>
            <form onSubmit={(e) => e.preventDefault()} className="flex border-b border-on-tertiary-container pb-2">
              <input 
                className="bg-transparent border-none focus:ring-0 w-full text-white placeholder:text-on-tertiary-container/50 outline-none" 
                placeholder="Email của bạn" 
                type="email"
                required
              />
              <button type="submit" className="text-white hover:text-primary bg-transparent border-none cursor-pointer">
                <span className="material-symbols-outlined">send</span>
              </button>
            </form>
          </div>
        </div>

        <div className="md:col-span-12 border-t border-on-tertiary-container/20 pt-8 mt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="font-label-sm text-label-sm text-on-tertiary-container uppercase tracking-widest">© 2026 ELYSIAN HOTELS</p>
          <div className="flex gap-8">
            <a className="font-label-sm text-label-sm text-on-tertiary-container hover:text-white uppercase no-underline" href="#">Điều khoản</a>
            <a className="font-label-sm text-label-sm text-on-tertiary-container hover:text-white uppercase no-underline" href="#">Bảo mật</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
