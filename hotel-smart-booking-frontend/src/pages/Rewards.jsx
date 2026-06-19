import { useState } from 'react';
import { useLanguage } from '../context/LanguageContext';

export default function Rewards({ setActivePage }) {
  const { t } = useLanguage();
  const [activeCard, setActiveCard] = useState(null);

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

  return (
    <div className="w-full">
      <main className="pt-4 text-left">
        {/* Hero Reward Section */}
        <section className="elysian-pattern relative py-stack-lg md:py-24 px-margin-mobile md:px-margin-desktop min-h-[819px] flex items-center justify-center">
          <div className="bg-surface-container-lowest w-full max-w-6xl mx-auto p-8 md:p-16 relative z-10 border border-outline-variant shadow-none">
            <div className="text-center space-y-stack-md">
              <h1 className="font-headline-xl text-headline-xl-mobile md:text-headline-xl text-primary max-w-3xl mx-auto leading-tight">
                {t('rewards_title')}
              </h1>
              <p className="font-body-lg text-body-lg text-secondary italic max-w-2xl mx-auto">
                {t('rewards_quote')}
              </p>
            </div>

            {/* Benefits Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter mt-stack-lg">
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
                onClick={() => setActivePage && setActivePage('register')}
                className="bg-primary text-on-primary px-12 py-5 font-label-bold uppercase tracking-widest hover:scale-105 transition-all duration-300 cursor-pointer border-none"
              >
                {t('rewards_btn_register')}
              </button>
            </div>
          </div>
        </section>

        {/* Dynamic Visual Section */}
        <section className="py-stack-lg bg-surface">
          <div className="max-w-[1440px] mx-auto px-4 md:px-margin-desktop grid md:grid-cols-2 gap-stack-lg items-center py-8">
            <div className="relative">
              <div className="absolute -top-4 -left-4 w-full h-full border-2 border-primary z-0"></div>
              <img 
                alt="Modern Hotel Interior" 
                className="relative z-10 w-full aspect-video object-cover grayscale hover:grayscale-0 transition-all duration-700" 
                src="https://images.unsplash.com/photo-1540518614846-7eded433c457?w=800&q=80"
              />
            </div>
            <div className="space-y-stack-md">
              <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase tracking-tight m-0">{t('rewards_lifestyle_title')}</h2>
              <p className="font-body-md text-body-md text-secondary">
                {t('rewards_lifestyle_desc')}
              </p>
              <div className="flex items-center gap-4 text-primary font-label-bold uppercase cursor-pointer hover:gap-6 transition-all">
                <span>{t('rewards_btn_discover_more')}</span>
                <span className="material-symbols-outlined">trending_flat</span>
              </div>
            </div>
          </div>
        </section>

        {/* Member Tiers */}
        <section className="py-stack-lg bg-surface-container-low border-y border-outline-variant">
          <div className="max-w-[1440px] mx-auto px-4 md:px-margin-desktop py-8">
            <div className="grid md:grid-cols-3 gap-gutter">
              <div className="p-8 border-l-4 border-primary bg-surface-container-lowest text-left">
                <span className="text-secondary font-label-bold text-sm block mb-1">01</span>
                <h4 className="font-headline-md text-headline-md mt-2 m-0">{t('rewards_tier1_title')}</h4>
                <p className="mt-4 text-secondary">{t('rewards_tier1_desc')}</p>
              </div>
              <div className="p-8 border-l-4 border-primary bg-surface-container-lowest text-left">
                <span className="text-secondary font-label-bold text-sm block mb-1">02</span>
                <h4 className="font-headline-md text-headline-md mt-2 m-0">{t('rewards_tier2_title')}</h4>
                <p className="mt-4 text-secondary">{t('rewards_tier2_desc')}</p>
              </div>
              <div className="p-8 border-l-4 border-primary bg-surface-container-lowest text-left">
                <span className="text-secondary font-label-bold text-sm block mb-1">03</span>
                <h4 className="font-headline-md text-headline-md mt-2 m-0">{t('rewards_tier3_title')}</h4>
                <p className="mt-4 text-secondary">{t('rewards_tier3_desc')}</p>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
