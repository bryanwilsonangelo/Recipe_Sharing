-- 1. Dummy Data: 2 Admin & 2 User 
INSERT INTO users (username, full_name, password, role) VALUES
('bryan_admin', 'Bryan Putra', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi71dM7G2t7j5i6J2Q8q5w5z5y5x5w5', 'ADMIN'),
('ken_admin', 'Ken Wijaya', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi71dM7G2t7j5i6J2Q8q5w5z5y5x5w5', 'ADMIN'),
('abram_user', 'Abram Santoso', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi71dM7G2t7j5i6J2Q8q5w5z5y5x5w5', 'USER'),
('clark_user', 'Clark Halim', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi71dM7G2t7j5i6J2Q8q5w5z5y5x5w5', 'USER');

-- 2. Dummy Data: 5 Kategori
INSERT INTO categories (name) VALUES
('Pedas'),
('Vegetarian'),
('Sarapan'),
('Cepat Saji'),
('Minuman');

-- 3. Dummy Data: 5 Resep 
INSERT INTO recipes (title, short_description, instruction, photo_path, idusr) VALUES
('Nasi Goreng Gila', 'Nasi goreng pedas dengan topping sosis dan bakso melimpah.', '1. Tumis bawang putih dan cabai.\n2. Masukkan telur, sosis, dan bakso.\n3. Masukkan nasi dan kecap, aduk hingga matang.', 'uploads/nasgor.jpg', 3),
('Salad Sayur Segar', 'Campuran selada, tomat ceri, dan timun dengan dressing wijen.', '1. Cuci bersih semua sayuran.\n2. Potong dadu tomat dan timun.\n3. Campurkan semua bahan dan siram saus wijen.', 'uploads/salad.jpg', 3),
('Roti Bakar Coklat Keju', 'Menu sarapan praktis roti panggang isi coklat dan parutan keju.', '1. Olesi roti dengan mentega.\n2. Panggang di teflon hingga kecoklatan.\n3. Beri meses coklat dan keju parut.', 'uploads/rotibakar.jpg', 4),
('Tahu Cabe Garam', 'Tahu goreng krispi ditumis dengan bawang putih dan cabai rawit.', '1. Potong dadu tahu dan baluri tepung.\n2. Goreng hingga garing.\n3. Tumis bawang putih, cabai, dan daun bawang, lalu masukkan tahu.', 'uploads/tahucabegaram.jpg', 4),
('Es Teh Leci', 'Minuman teh manis dingin dengan tambahan buah leci segar.', '1. Seduh teh dengan air panas.\n2. Tambahkan gula sesuai selera.\n3. Masukkan es batu dan buah leci beserta sirupnya.', 'uploads/estehleci.jpg', 3);

-- 4. Dummy Data: Relasi Resep & Kategori 
INSERT INTO recipes_categories (idrec, idcat) VALUES
(1, 1), -- Nasi Goreng Gila -> Pedas
(1, 4), -- Nasi Goreng Gila -> Cepat Saji
(2, 2), -- Salad Sayur Segar -> Vegetarian
(2, 3), -- Salad Sayur Segar -> Sarapan
(3, 3), -- Roti Bakar Coklat Keju -> Sarapan
(3, 4), -- Roti Bakar Coklat Keju -> Cepat Saji
(4, 1), -- Tahu Cabe Garam -> Pedas
(4, 2), -- Tahu Cabe Garam -> Vegetarian
(5, 5); -- Es Teh Leci -> Minuman