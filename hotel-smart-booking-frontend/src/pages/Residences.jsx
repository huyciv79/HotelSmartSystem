export default function Residences() {
  return (
    <div className="w-full">
      <main className="pt-24 pb-stack-lg text-left">
        <header className="px-margin-mobile md:px-margin-desktop mb-stack-lg text-center">
          <h1 className="font-headline-xl text-headline-xl md:text-headline-xl mb-stack-sm text-on-surface">Elysian Residences</h1>
          <div className="max-w-4xl mx-auto border-t border-outline-variant pt-8">
            <p className="font-body-lg text-body-lg text-secondary">
              Không gian sống và làm việc hiện đại, năng động dành cho các nhà sáng tạo nội dung, doanh nhân và cư dân đô thị thế hệ mới.
            </p>
          </div>
        </header>

        <section className="px-margin-mobile md:px-margin-desktop max-w-[1440px] mx-auto py-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="font-headline-lg text-headline-lg text-primary mb-6">Định nghĩa lại Co-Living</h2>
              <p className="font-body-lg text-body-lg text-secondary mb-6">
                Elysian Residences kết hợp sự sang trọng riêng tư của căn hộ cao cấp với sự kết nối năng động của không gian sinh hoạt chung.
              </p>
              <ul className="space-y-4 font-label-bold text-label-bold text-on-surface list-none p-0">
                <li className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">check_circle</span>
                  TIỆN NGHI CAO CẤP 24/7
                </li>
                <li className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">check_circle</span>
                  CỘNG ĐỒNG BẢN ĐỊA NĂNG ĐỘNG
                </li>
                <li className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">check_circle</span>
                  THỦ TỤC THUÊ NHANH GỌN, LINH HOẠT
                </li>
              </ul>
            </div>
            <div className="elysian-parallelogram overflow-hidden shadow-2xl">
              <img 
                alt="Elysian Residences Interior" 
                className="w-full h-[400px] object-cover hover:scale-105 transition-transform duration-700" 
                src="https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80"
              />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
