import { ShieldCheck, CreditCard, Smartphone } from "lucide-react";

const IdentityCard = () => {
  return (
    <div className="p-8 bg-[#111111] rounded-none border border-neutral-800 border-l-4 border-l-primary shadow-xl relative overflow-hidden font-['Montserrat']">
      <div className="absolute size-40 bg-primary rounded-full blur-[48px] opacity-10 left-[170px] top-[169px]" />

      <div className="relative z-10 flex flex-col gap-6">
        <div className="flex justify-between items-center">
          <h2 className="text-white font-bold text-xl uppercase tracking-wider">Identity Verified</h2>
          <ShieldCheck className="text-primary size-6" />
        </div>

        <p className="text-neutral-400 text-xs font-medium leading-relaxed uppercase tracking-wider">
          Your digital keys and seamless check-in are enabled.
        </p>

        <div className="flex gap-3">
          <div className="w-24 h-16 bg-white/5 rounded-none border border-white/10 flex items-center justify-center hover:border-primary/50 transition-colors">
            <CreditCard className="text-white/60 size-5" />
          </div>
          <div className="w-24 h-16 bg-white/5 rounded-none border border-white/10 flex items-center justify-center hover:border-primary/50 transition-colors">
            <Smartphone className="text-white/60 size-5" />
          </div>
        </div>

        <div className="pt-2 border-t border-white/10">
          <p className="text-white/40 text-[9px] uppercase tracking-widest font-bold">
            Verified on Sept 14, 2024
          </p>
        </div>
      </div>
    </div>
  );
};

export default IdentityCard;
