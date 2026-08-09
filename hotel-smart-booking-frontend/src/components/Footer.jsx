import { useLanguage } from '../context/LanguageContext';

export default function Footer({ activePage, setActivePage }) {
  const { t } = useLanguage();

  const handleNavClick = (id) => {
    setActivePage(id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="bg-inverse-surface text-on-primary-container py-14 px-4 md:px-margin-desktop mt-auto border-t border-white/10 font-['Montserrat']">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-8 text-left">
        
        {/* IRIS Signature & Brand */}
        <div className="lg:col-span-5 space-y-4">
          <div className="flex items-center gap-3">
            <h2 className="font-headline-md text-2xl font-black text-white tracking-wider uppercase m-0">
              ELYSIAN
            </h2>
            <span className="text-[10px] font-black tracking-widest text-amber-400 uppercase bg-amber-400/10 px-2.5 py-1 border border-amber-400/30 rounded-md">
              {t('footer_iris_signature', 'The IRIS Signature')}
            </span>
          </div>
          <p className="font-body-md text-xs md:text-sm text-slate-300/90 leading-relaxed max-w-md">
            {t(
              'footer_iris_desc',
              'Chúng tôi rất hân hạnh được hỗ trợ bạn mọi lúc. Hãy liên hệ để nhận tư vấn đặt phòng, thông tin dịch vụ hoặc hỗ trợ trực tiếp. Dịch vụ thân thiện và chuyên nghiệp luôn sẵn sàng phục vụ.'
            )}
          </p>
          <div className="pt-2">
            <button 
              onClick={() => handleNavClick('register')}
              className="px-6 py-2.5 border border-amber-400/50 text-amber-300 font-black text-[11px] uppercase tracking-widest hover:bg-amber-400 hover:text-slate-950 transition-all duration-300 cursor-pointer rounded-lg shadow-sm"
            >
              {t('footer_register_now', 'Đăng ký ngay')}
            </button>
          </div>
        </div>

        {/* Contact Information */}
        <div className="lg:col-span-4 space-y-4">
          <h3 className="font-label-md text-xs font-black text-white tracking-widest uppercase mb-4 text-amber-400 border-b border-white/10 pb-2">
            {t('footer_contact_info', 'THÔNG TIN LIÊN HỆ')}
          </h3>
          <ul className="space-y-3.5 p-0 m-0 list-none text-xs font-medium text-slate-300">
            <li className="flex items-start gap-3">
              <span className="material-symbols-outlined text-amber-400 text-base shrink-0 mt-0.5">call</span>
              <div>
                <span className="block text-[10px] uppercase font-bold text-slate-400 tracking-wider">Số điện thoại</span>
                <a href="tel:+842923686969" className="text-white hover:text-amber-300 font-bold transition-colors no-underline">
                  (+84 292) 368 6969
                </a>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="material-symbols-outlined text-amber-400 text-base shrink-0 mt-0.5">chat</span>
              <div>
                <span className="block text-[10px] uppercase font-bold text-slate-400 tracking-wider">Zalo</span>
                <a href="https://zalo.me/0839999521" target="_blank" rel="noopener noreferrer" className="text-white hover:text-amber-300 font-bold transition-colors no-underline">
                  (+84) 83 9999 521
                </a>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="material-symbols-outlined text-amber-400 text-base shrink-0 mt-0.5">mail</span>
              <div>
                <span className="block text-[10px] uppercase font-bold text-slate-400 tracking-wider">Email</span>
                <a href="mailto:info@irishotelcantho.vn" className="text-white hover:text-amber-300 font-bold transition-colors no-underline">
                  info@irishotelcantho.vn
                </a>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <span className="material-symbols-outlined text-amber-400 text-base shrink-0 mt-0.5">location_on</span>
              <div>
                <span className="block text-[10px] uppercase font-bold text-slate-400 tracking-wider">Địa chỉ</span>
                <span className="text-white font-medium leading-relaxed block">
                  224 Đường 30/4, Phường Ninh Kiều, Thành phố Cần Thơ, Việt Nam.
                </span>
              </div>
            </li>
          </ul>
        </div>

        {/* Explore & Legal Navigation */}
        <div className="lg:col-span-3 grid grid-cols-2 gap-4">
          {/* Explore */}
          <div className="flex flex-col gap-3">
            <h3 className="font-label-md text-xs font-black text-white tracking-widest uppercase mb-2 text-amber-400 border-b border-white/10 pb-2">
              {t('footer_explore', 'KHÁM PHÁ')}
            </h3>
            <button 
              onClick={() => handleNavClick('home')}
              className={`font-label-sm text-xs uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
                activePage === 'home' 
                  ? 'text-white border-b border-amber-400 font-bold' 
                  : 'text-slate-300 hover:text-white border-b border-transparent'
              }`}
            >
              {t('nav_hotels', 'KHÁCH SẠN')}
            </button>
            <button 
              onClick={() => handleNavClick('residences')}
              className={`font-label-sm text-xs uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
                activePage === 'residences' 
                  ? 'text-white border-b border-amber-400 font-bold' 
                  : 'text-slate-300 hover:text-white border-b border-transparent'
              }`}
            >
              {t('nav_residences', 'RESIDENCES')}
            </button>
            <button 
              onClick={() => handleNavClick('experiences')}
              className={`font-label-sm text-xs uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
                activePage === 'experiences' 
                  ? 'text-white border-b border-amber-400 font-bold' 
                  : 'text-slate-300 hover:text-white border-b border-transparent'
              }`}
            >
              {t('nav_experiences', 'TRẢI NGHIỆM')}
            </button>
          </div>

          {/* Legal */}
          <div className="flex flex-col gap-3">
            <h3 className="font-label-md text-xs font-black text-white tracking-widest uppercase mb-2 text-amber-400 border-b border-white/10 pb-2">
              {t('footer_legal', 'PHÁP LÝ')}
            </h3>
            <button 
              onClick={() => handleNavClick('terms-of-service')}
              className={`font-label-sm text-xs uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
                activePage === 'terms-of-service' 
                  ? 'text-white border-b border-amber-400 font-bold' 
                  : 'text-slate-300 hover:text-white border-b border-transparent'
              }`}
            >
              {t('footer_terms', 'ĐIỀU KHOẢN')}
            </button>
            <button 
              onClick={() => handleNavClick('privacy-policy')}
              className={`font-label-sm text-xs uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
                activePage === 'privacy-policy' 
                  ? 'text-white border-b border-amber-400 font-bold' 
                  : 'text-slate-300 hover:text-white border-b border-transparent'
              }`}
            >
              {t('footer_privacy', 'BẢO MẬT')}
            </button>
          </div>
        </div>

        {/* Sub-footer Copyright */}
        <div className="lg:col-span-12 border-t border-white/10 pt-6 mt-4 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="font-label-sm text-[11px] text-slate-400 uppercase tracking-widest m-0">
            © Elysian Hotel Cần Thơ - 224 Đường 30/4, Phường Ninh Kiều, Thành phố Cần Thơ, Việt Nam.
          </p>
          <div className="flex gap-4 text-amber-400 text-xs font-black uppercase tracking-wider">
            <span>The IRIS Signature</span>
          </div>
        </div>

      </div>
    </footer>
  );
}
