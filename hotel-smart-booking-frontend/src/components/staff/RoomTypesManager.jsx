import { useState } from 'react';

const RoomTypesManager = ({
  roomTypes,
  isRoomFormOpen,
  setIsRoomFormOpen,
  editingRoom,
  roomFormData,
  setRoomFormData,
  handleFileChange,
  handleRoomSubmit,
  handleOpenAddRoom,
  handleOpenEditRoom,
  handleDeleteRoom,
  isSubmittingRoom
}) => {
  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="flex justify-between items-end border-b border-neutral-900 pb-4">
        <div>
          <h3 className="text-white font-black text-base uppercase tracking-wider m-0">QUẢN LÝ LOẠI PHÒNG</h3>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Cấu hình thông tin danh mục phòng ngủ và đơn giá</p>
        </div>

        <button
          onClick={handleOpenAddRoom}
          className="bg-primary text-white font-black text-xs uppercase tracking-widest px-6 py-3 border-none cursor-pointer hover:brightness-110 active:scale-98 transition-all flex items-center gap-1.5"
        >
          Thêm loại phòng
        </button>
      </div>

      {/* Room Form Overlay / Collapsible form */}
      {isRoomFormOpen && (
        <div className="bg-white border border-slate-200/85 p-6 md:p-8 shadow-md">
          <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
            {editingRoom ? `CHỈNH SỬA LOẠI PHÒNG: ${editingRoom.name}` : 'THÊM MỚI LOẠI PHÒNG'}
          </h4>

          <form onSubmit={handleRoomSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Tên loại phòng</label>
                <input
                  type="text"
                  value={roomFormData.name}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="E.g., Suite River View"
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Đơn giá cơ bản (VND / đêm)</label>
                <input
                  type="number"
                  value={roomFormData.basePrice}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, basePrice: e.target.value }))}
                  placeholder="E.g., 5000000"
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Sức chứa người lớn / phòng</label>
                <input
                  type="number"
                  value={roomFormData.adultCapacity}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, adultCapacity: e.target.value }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Sức chứa trẻ em / phòng</label>
                <input
                  type="number"
                  value={roomFormData.childCapacity}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, childCapacity: e.target.value }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Loại giường</label>
                <input
                  type="text"
                  value={roomFormData.bedType}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, bedType: e.target.value }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Diện tích phòng (m²)</label>
                <input
                  type="number"
                  value={roomFormData.roomSize}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, roomSize: e.target.value }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Trạng thái hoạt động</label>
                <select
                  value={roomFormData.status}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, status: e.target.value }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary [&>option]:bg-white [&>option]:text-slate-800"
                >
                  <option value="Active">Active</option>
                  <option value="Inactive">Inactive</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Hình ảnh phòng</label>
                <input
                  type="file"
                  onChange={handleFileChange}
                  className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:border file:border-slate-200 file:text-[10px] file:font-black file:uppercase file:bg-slate-50 file:text-slate-800 hover:file:bg-slate-100 cursor-pointer"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Mô tả loại phòng</label>
              <textarea
                rows="3"
                value={roomFormData.description}
                onChange={(e) => setRoomFormData(prev => ({ ...prev, description: e.target.value }))}
                className="w-full bg-transparent border border-slate-200 p-3 font-bold text-xs outline-none text-slate-800 focus:border-primary resize-none"
              />
            </div>

            <div className="flex gap-4">
              <button
                type="submit"
                disabled={isSubmittingRoom}
                className="bg-primary text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5"
              >
                {isSubmittingRoom ? 'Đang lưu...' : 'Lưu lại'}
              </button>
              <button
                type="button"
                onClick={() => setIsRoomFormOpen(false)}
                className="bg-slate-50 hover:bg-slate-100 text-slate-800 font-bold px-8 py-3.5 uppercase text-xs tracking-widest border border-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Room Types table */}
      <div className="bg-white border border-slate-200/85 shadow-sm overflow-hidden">
        <table className="w-full border-collapse text-left text-xs text-slate-600">
          <thead>
            <tr className="border-b border-slate-100 text-[9px] font-black uppercase tracking-wider text-slate-500">
              <th className="p-4">Hình ảnh</th>
              <th className="p-4">Tên loại phòng</th>
              <th className="p-4">Giá / đêm</th>
              <th className="p-4">Giường / Diện tích</th>
              <th className="p-4">Sức chứa tối đa</th>
              <th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {roomTypes.map((room) => (
              <tr key={room.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="p-4">
                  <img
                    src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=100&q=80'}
                    alt={room.name}
                    className="w-16 h-12 object-cover border border-slate-200"
                  />
                </td>
                <td className="p-4">
                  <span className="text-slate-800 font-black uppercase text-sm block">{room.name}</span>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider block mt-0.5">ID: {room.id}</span>
                </td>
                <td className="p-4 text-primary font-black">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.basePrice || room.baseprice || 0)}
                </td>
                <td className="p-4 font-semibold text-slate-500">
                  {room.bedType || 'King Bed'} <br />
                  {room.area || room.roomSize || room.roomsize || 35} m²
                </td>
                <td className="p-4">
                  {room.adultCapacity || room.adultcapacity || 2} NL • {room.childCapacity || room.childcapacity || 1} TE
                </td>
                <td className="p-4">
                  <span className={`px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                    room.status === 'Active' || !room.status 
                      ? 'bg-green-50 text-green-700 border border-green-200' 
                      : 'bg-red-50 text-red-600 border border-red-200'
                  }`}>
                    {room.status || 'Active'}
                  </span>
                </td>
                <td className="p-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => handleOpenEditRoom(room)}
                        className="bg-slate-100 border border-slate-200 text-slate-700 hover:bg-slate-200 hover:text-slate-900 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer rounded-sm transition-all"
                      >
                        Sửa
                      </button>
                      <button
                        onClick={() => handleDeleteRoom(room.id, room.name)}
                        className="bg-rose-50 border border-rose-200 text-rose-700 hover:bg-rose-100 hover:border-rose-350 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer rounded-sm transition-all"
                      >
                        Xóa
                      </button>
                    </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default RoomTypesManager;
