import { useLanguage } from '../context/LanguageContext';

export default function Footer({ activePage, setActivePage }) {
  const { t } = useLanguage();

  const handleNavClick = (id) => {
    setActivePage(id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="bg-inverse-surface text-on-primary-container py-stack-lg px-4 md:px-margin-desktop mt-auto">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-12 gap-gutter">
        {/* Brand Column */}
        <div className="md:col-span-6 space-y-8 text-left">
          <h2 className="font-headline-md text-headline-md text-white tracking-tighter">ELYSIAN</h2>
          <p className="font-body-md text-body-md text-on-tertiary-container max-w-sm">
            {t('footer_desc')}
          </p>
          <button 
            onClick={() => handleNavClick('register')}
            className="px-6 py-3 border border-on-tertiary-container text-white font-bold font-['Montserrat'] text-xs uppercase tracking-widest hover:bg-white hover:text-neutral-900 hover:border-white transition-all duration-300 cursor-pointer rounded-none"
          >
            {t('footer_register_now', 'Đăng ký ngay')}
          </button>
        </div>

        {/* Explore Column */}
        <div className="md:col-span-3 flex flex-col gap-4 text-left">
          <h3 className="font-label-md text-label-md text-white tracking-widest uppercase mb-stack-sm">
            {t('footer_explore', 'KHÁM PHÁ')}
          </h3>
          <button 
            onClick={() => handleNavClick('home')}
            className={`font-label-sm text-label-sm uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
              activePage === 'home' 
                ? 'text-white border-b border-white' 
                : 'text-on-tertiary-container hover:text-white border-b border-transparent'
            }`}
          >
            {t('nav_hotels', 'KHÁCH SẠN')}
          </button>
          <button 
            onClick={() => handleNavClick('residences')}
            className={`font-label-sm text-label-sm uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
              activePage === 'residences' 
                ? 'text-white border-b border-white' 
                : 'text-on-tertiary-container hover:text-white border-b border-transparent'
            }`}
          >
            {t('nav_residences', 'ELYSIAN RESIDENCES')}
          </button>
          <button 
            onClick={() => handleNavClick('experiences')}
            className={`font-label-sm text-label-sm uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
              activePage === 'experiences' 
                ? 'text-white border-b border-white' 
                : 'text-on-tertiary-container hover:text-white border-b border-transparent'
            }`}
          >
            {t('nav_experiences', 'TRẢI NGHIỆM ELYSIAN')}
          </button>
        </div>

        {/* Legal Column */}
        <div className="md:col-span-3 flex flex-col gap-4 text-left">
          <h3 className="font-label-md text-label-md text-white tracking-widest uppercase mb-stack-sm">
            {t('footer_legal', 'PHÁP LÝ')}
          </h3>
          <button 
            onClick={() => handleNavClick('terms-of-service')}
            className={`font-label-sm text-label-sm uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
              activePage === 'terms-of-service' 
                ? 'text-white border-b border-white' 
                : 'text-on-tertiary-container hover:text-white border-b border-transparent'
            }`}
          >
            {t('footer_terms', 'ĐIỀU KHOẢN')}
          </button>
          <button 
            onClick={() => handleNavClick('privacy-policy')}
            className={`font-label-sm text-label-sm uppercase no-underline bg-transparent border-none p-0 cursor-pointer text-left w-fit transition-all pb-0.5 ${
              activePage === 'privacy-policy' 
                ? 'text-white border-b border-white' 
                : 'text-on-tertiary-container hover:text-white border-b border-transparent'
            }`}
          >
            {t('footer_privacy', 'BẢO MẬT')}
          </button>
        </div>

        {/* Sub-footer Copyright */}
        <div className="md:col-span-12 border-t border-on-tertiary-container/20 pt-8 mt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="font-label-sm text-label-sm text-on-tertiary-container uppercase tracking-widest">© 2026 ELYSIAN HOTELS</p>
        </div>
      </div>
    </footer>
  );
}
