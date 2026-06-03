const PaymentOverview = ({ payment }) => {
  return (
    <div className="p-8 bg-white rounded-xl shadow-sm outline outline-1 outline-neutral-300/30 flex flex-col gap-8">
      <p className="text-zinc-900 text-lg font-bold font-['Geist']">
        Payment Overview
      </p>

      <div className="h-32 flex justify-center items-end gap-2">
        {payment.bars.map((bar, index) => (
          <div
            key={index}
            className={`flex-1 rounded-tl-md rounded-tr-md ${bar.color} hover:opacity-80 transition`}
            style={{ height: bar.height }}
          />
        ))}
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex justify-between">
          <div className="flex items-center gap-2">
            <div className="size-3 bg-black rounded-full" />
            <span className="text-zinc-700 text-sm font-['Geist']">
              Paid Amount
            </span>
          </div>
          <span className="text-zinc-900 font-bold font-['Geist']">
            {payment.paidAmount}
          </span>
        </div>

        <div className="flex justify-between">
          <div className="flex items-center gap-2">
            <div className="size-3 bg-amber-200 rounded-full" />
            <span className="text-zinc-700 text-sm font-['Geist']">
              Pending Balance
            </span>
          </div>
          <span className="text-zinc-900 font-bold font-['Geist']">
            {payment.pendingBalance}
          </span>
        </div>
      </div>
    </div>
  );
};

export default PaymentOverview;
