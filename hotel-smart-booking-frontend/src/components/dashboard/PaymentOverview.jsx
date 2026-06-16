const PaymentOverview = ({ payment }) => {
  return (
    <div className="p-8 bg-white rounded-none border border-outline-variant shadow-lg flex flex-col gap-8 font-['Montserrat'] text-left">
      <p className="text-zinc-950 text-sm font-black uppercase tracking-wider border-b border-gray-150 pb-2">
        Tổng quan thanh toán
      </p>

      <div className="h-32 flex justify-center items-end gap-2.5">
        {payment.bars.map((bar, index) => {
          const barColor = bar.color === 'bg-amber-200' ? 'bg-primary' : bar.color;
          return (
            <div
              key={index}
              className={`flex-1 rounded-none ${barColor} hover:opacity-80 transition`}
              style={{ height: bar.height }}
            />
          );
        })}
      </div>

      <div className="flex flex-col gap-4 text-xs font-bold">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="size-3 bg-black rounded-none" />
            <span className="text-slate-500 uppercase tracking-wider">
              Đã thanh toán
            </span>
          </div>
          <span className="text-slate-900 font-black">
            {payment.paidAmount}
          </span>
        </div>

        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="size-3 bg-primary rounded-none" />
            <span className="text-slate-500 uppercase tracking-wider">
              Dư nợ chờ thanh toán
            </span>
          </div>
          <span className="text-primary font-black">
            {payment.pendingBalance}
          </span>
        </div>
      </div>
    </div>
  );
};

export default PaymentOverview;
