import { useLanguage } from '../context/LanguageContext';

export default function Events() {
  const { t } = useLanguage();

  return (
    <div className="w-full">
      <main className="pt-24 pb-stack-lg text-left">
        <header className="px-margin-mobile md:px-margin-desktop mb-stack-lg text-center">
          <h1 className="font-headline-xl text-headline-xl md:text-headline-xl mb-stack-sm text-on-surface">{t('events_title')}</h1>
          <div className="max-w-4xl mx-auto border-t border-outline-variant pt-8">
            <p className="font-body-lg text-body-lg text-secondary">
              {t('events_desc')}
            </p>
          </div>
        </header>

        <section className="px-margin-mobile md:px-margin-desktop max-w-[1440px] mx-auto py-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            <div className="elysian-trapezoid-right overflow-hidden shadow-2xl order-2 md:order-1">
              <img 
                alt="Elysian Events Room" 
                className="w-full h-[400px] object-cover hover:scale-105 transition-transform duration-700" 
                src="https://images.unsplash.com/photo-1511578314322-379afb476865?w=800&q=80"
              />
            </div>
            <div className="order-1 md:order-2">
              <h2 className="font-headline-lg text-headline-lg text-primary mb-6">{t('events_smart_space')}</h2>
              <p className="font-body-lg text-body-lg text-secondary mb-6">
                {t('events_smart_space_desc')}
              </p>
              <div className="flex gap-4">
                <button className="bg-primary text-on-primary px-8 py-4 font-label-bold uppercase tracking-widest hover:scale-105 transition-all cursor-pointer border-none">
                  {t('events_btn_contact')}
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
