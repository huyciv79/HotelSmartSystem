import { useEffect, useState } from 'react';
import { useLanguage } from '../context/LanguageContext';
import Rewards from './Rewards';

export default function Experiences({ setActivePage }) {
  const { t } = useLanguage();
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
          {t('exp_tab_services')}
        </button>
        <button
          onClick={() => setSubTab('rewards')}
          className={`font-label-bold text-label-bold uppercase tracking-wider py-2 px-4 cursor-pointer bg-transparent border-none ${
            subTab === 'rewards'
              ? 'text-primary border-b-2 border-primary font-bold'
              : 'text-on-surface hover:text-primary border-b-2 border-transparent'
          }`}
        >
          {t('exp_tab_rewards')}
        </button>
      </div>

      {subTab === 'services' ? (
        <div>
          {/* Hero / Section 1: Intro */}
          <section className="relative bg-surface-container-low py-stack-lg overflow-hidden text-left">
            <div className="max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop grid grid-cols-1 md:grid-cols-12 gap-gutter items-center animate-on-scroll">
              <div className="md:col-span-5 z-10">
                <h1 className="font-headline-xl text-headline-xl uppercase italic mb-stack-sm leading-tight">
                  {t('exp_tech_title1')} <br /> <span className="text-primary">{t('exp_tech_title2')}</span>
                </h1>
                <p className="font-body-lg text-body-lg text-secondary italic uppercase tracking-widest">
                  {t('exp_tech_desc')}
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
                  <p className="font-body-md text-body-md text-secondary italic">{t('exp_stay24_desc')}</p>
                </div>
                {/* Grab & Go Right Content */}
                <div className="order-1 md:order-2 text-right">
                  <h2 className="font-headline-md text-headline-md uppercase italic">{t('exp_service247_desc').split(',')[0]}</h2>
                  <p className="font-body-md text-body-md text-secondary italic mb-stack-md max-w-sm ml-auto">
                    {t('exp_service247_desc')}
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
                  <h3 className="font-headline-md text-headline-md mb-2">{t('exp_checkin_title')}</h3>
                  <p className="font-body-md text-body-md text-tertiary">
                    {t('exp_checkin_desc')}
                  </p>
                </div>
                <div className="bg-inverse-surface p-stack-md text-white">
                  <span className="material-symbols-outlined text-primary-fixed text-4xl mb-4">hub</span>
                  <h3 className="font-headline-md text-headline-md mb-2 text-white">{t('exp_hub_title')}</h3>
                  <p className="font-body-md text-body-md text-secondary-fixed">
                    {t('exp_hub_desc')}
                  </p>
                </div>
                <div className="bg-surface-container p-stack-md border-r-4 border-primary text-right">
                  <span className="material-symbols-outlined text-primary text-4xl mb-4">local_bar</span>
                  <h3 className="font-headline-md text-headline-md mb-2">{t('exp_bar_title')}</h3>
                  <p className="font-body-md text-body-md text-tertiary">
                    {t('exp_bar_desc')}
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
              <h2 className="font-headline-xl text-headline-xl text-white uppercase italic mb-stack-md">{t('exp_ready_title')}</h2>
              <button 
                onClick={() => setActivePage('offers')}
                className="bg-primary hover:bg-surface-tint text-white px-12 py-5 font-label-bold text-label-bold uppercase tracking-[0.2em] transition-all duration-300 transform hover:skew-x-[-12deg] cursor-pointer border-none"
              >
                {t('exp_btn_discover')}
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
