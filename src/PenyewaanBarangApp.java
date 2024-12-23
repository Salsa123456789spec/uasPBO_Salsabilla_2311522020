import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

// Koneksi database
class DatabaseHelper {
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/db_penyewaan", "root", "");
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        }
    }
}

// Interface untuk tindakan user dan admin
interface UserActions {
    void sewaBarang();
    void lihatBarang();
}

// Kelas Admin yang mewarisi UserActions
class Admin implements UserActions {
    private Scanner scanner = new Scanner(System.in);
    private Connection conn = DatabaseHelper.getConnection();

    @Override
    public void sewaBarang() {
        // Admin tidak menyewa barang
    }

    @Override
    public void lihatBarang() {
        String sql = "SELECT * FROM barang";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===== Daftar Barang =====");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id_barang") + 
                                   " | Nama: " + rs.getString("nama_barang") + 
                                   " | Harga: " + rs.getInt("harga") + 
                                   " | Jumlah: " + rs.getInt("jumlah"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Menambahkan barang
    public void addBarang() {
        System.out.print("Masukkan Nama Barang: ");
        String namaBarang = scanner.next().toLowerCase();

        // Validasi kategori barang
        if (!namaBarang.equals("kamera") && 
            !namaBarang.equals("handphone") && 
            !namaBarang.equals("proyektor") && 
            !namaBarang.equals("laptop")) {
            System.out.println("Barang hanya boleh: kamera, handphone, proyektor, laptop.");
            return;
        }

        System.out.print("Masukkan Harga Barang: ");
        int harga = scanner.nextInt();
        System.out.print("Masukkan Jumlah Barang: ");
        int jumlah = scanner.nextInt();

        String sql = "INSERT INTO barang (nama_barang, harga, jumlah) VALUES (?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, namaBarang);
            pst.setInt(2, harga);
            pst.setInt(3, jumlah);
            pst.executeUpdate();
            System.out.println("Barang berhasil ditambahkan!");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Melihat daftar penyewa
    public void lihatDaftarPenyewa() {
        String sql = "SELECT t.id, u.username, b.nama_barang, t.jumlah, t.tanggal_sewa, t.lama_sewa, t.total_harga " +
                     "FROM transaksi t " +
                     "JOIN barang b ON t.id_barang = b.id_barang " +
                     "JOIN user u ON t.id_user = u.id_user";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===== Daftar Penyewa =====");
            while (rs.next()) {
                System.out.println("ID Transaksi: " + rs.getInt("id") +
                                   " | Penyewa: " + rs.getString("username") +
                                   " | Barang: " + rs.getString("nama_barang") +
                                   " | Jumlah: " + rs.getInt("jumlah") +
                                   " | Tanggal Sewa: " + rs.getDate("tanggal_sewa") +
                                   " | Lama Sewa: " + rs.getInt("lama_sewa") +
                                   " | Total Harga: " + rs.getInt("total_harga"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Menghapus penyewa
    public void hapusPenyewa() {
        System.out.print("Masukkan ID Transaksi yang ingin dihapus: ");
        int idTransaksi = scanner.nextInt();

        String sql = "DELETE FROM transaksi WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idTransaksi);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Transaksi berhasil dihapus!");
            } else {
                System.out.println("Transaksi tidak ditemukan.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

// Kelas User yang mewarisi UserActions
class User implements UserActions {
    private Scanner scanner = new Scanner(System.in);
    private Connection conn = DatabaseHelper.getConnection();

    @Override
    public void sewaBarang() {
        System.out.print("Masukkan ID Barang yang ingin disewa: ");
        int idBarang = scanner.nextInt();
        System.out.print("Masukkan Jumlah Barang yang ingin disewa: ");
        int jumlahSewa = scanner.nextInt();
        System.out.print("Masukkan Lama Sewa (dalam hari): ");
        int lamaSewa = scanner.nextInt();

        String sqlCheck = "SELECT nama_barang, jumlah, harga FROM barang WHERE id_barang = ?";
        try (PreparedStatement pstCheck = conn.prepareStatement(sqlCheck)) {
            pstCheck.setInt(1, idBarang);
            ResultSet rsCheck = pstCheck.executeQuery();

            if (rsCheck.next()) {
                String namaBarang = rsCheck.getString("nama_barang").toLowerCase();

                // Validasi kategori barang
                if (!namaBarang.equals("kamera") && 
                    !namaBarang.equals("handphone") && 
                    !namaBarang.equals("proyektor") && 
                    !namaBarang.equals("laptop")) {
                    System.out.println("Barang yang dapat disewa hanya: kamera, handphone, proyektor, laptop.");
                    return;
                }

                int stok = rsCheck.getInt("jumlah");
                int hargaPerItem = rsCheck.getInt("harga");

                if (stok >= jumlahSewa) {
                    int totalHarga = hargaPerItem * jumlahSewa * lamaSewa;

                    // Menampilkan detail transaksi kepada user
                    System.out.println("\n===== Detail Transaksi =====");
                    System.out.println("Barang: " + namaBarang);
                    System.out.println("Harga per Item: Rp" + hargaPerItem);
                    System.out.println("Jumlah Barang: " + jumlahSewa);
                    System.out.println("Lama Sewa: " + lamaSewa + " hari");
                    System.out.println("Total Harga: Rp" + totalHarga);

                    System.out.print("Apakah Anda ingin melanjutkan transaksi? (y/n): ");
                    char konfirmasi = scanner.next().toLowerCase().charAt(0);
                    if (konfirmasi == 'y') {
                        // Mengurangi stok barang
                        String sqlUpdate = "UPDATE barang SET jumlah = jumlah - ? WHERE id_barang = ?";
                        try (PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdate)) {
                            pstUpdate.setInt(1, jumlahSewa);
                            pstUpdate.setInt(2, idBarang);
                            pstUpdate.executeUpdate();
                            System.out.println("Stok barang berhasil diperbarui.");
                        }

                        // Menyimpan transaksi
                        String sqlInsert = "INSERT INTO transaksi (id_barang, id_user, jumlah, tanggal_sewa, lama_sewa, total_harga) " +
                                           "VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstInsert = conn.prepareStatement(sqlInsert)) {
                            pstInsert.setInt(1, idBarang);
                            pstInsert.setInt(2, 1); // ID user, sesuaikan dengan sistem login
                            pstInsert.setInt(3, jumlahSewa);
                            pstInsert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            pstInsert.setInt(5, lamaSewa);
                            pstInsert.setInt(6, totalHarga);
                            pstInsert.executeUpdate();
                            System.out.println("Transaksi penyewaan berhasil disimpan!");
                        }
                    } else {
                        System.out.println("Transaksi dibatalkan.");
                    }
                } else {
                    System.out.println("Stok tidak cukup untuk disewa!");
                }
            } else {
                System.out.println("Barang tidak ditemukan!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    public void lihatBarang() {
        String sql = "SELECT * FROM barang";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===== Daftar Barang =====");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id_barang") + 
                                   " | Nama: " + rs.getString("nama_barang") + 
                                   " | Harga: " + rs.getInt("harga") + 
                                   " | Jumlah: " + rs.getInt("jumlah"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void registerUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Masukkan Username: ");
        String username = scanner.next();
        System.out.print("Masukkan Password: ");
        String password = scanner.next();

        String sql = "INSERT INTO user (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement pst = DatabaseHelper.getConnection().prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setString(3, "user");
            pst.executeUpdate();
            System.out.println("Pendaftaran berhasil!");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static UserActions loginUser(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        try (PreparedStatement pst = DatabaseHelper.getConnection().prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("admin".equals(role)) {
                    return new Admin();
                } else {
                    return new User();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }
}

// Kelas utama untuk menjalankan program
public class PenyewaanBarangApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserActions currentUser = null;

        while (true) {
            System.out.println("Pilih menu:");
            System.out.println("1. Login sebagai Admin");
            System.out.println("2. Login sebagai User");
            System.out.println("3. Register");
            System.out.println("4. Keluar");
            System.out.print("Pilih menu: ");
            int menu = scanner.nextInt();

            if (menu == 1 || menu == 2) {
                System.out.print("Username: ");
                String username = scanner.next();
                System.out.print("Password: ");
                String password = scanner.next();

                currentUser = User.loginUser(username, password);
                if (currentUser != null) {
                    break;
                } else {
                    System.out.println("Username atau password salah!");
                }
            } else if (menu == 3) {
                User.registerUser();
            } else if (menu == 4) {
                System.out.println("Terima kasih!");
                return;
            } else {
                System.out.println("Pilihan tidak valid.");
            }
        }

        if (currentUser != null) {
            while (true) {
                System.out.println("\nPilih menu:");
                System.out.println("1. Lihat Barang");
                System.out.println("2. Sewa Barang");
                if (currentUser instanceof Admin) {
                    System.out.println("3. Tambah Barang");
                    System.out.println("4. Lihat Daftar Penyewa");
                    System.out.println("5. Hapus Transaksi");
                }
                System.out.println("0. Keluar");
                System.out.print("Pilih menu: ");
                int menu = scanner.nextInt();

                switch (menu) {
                    case 1:
                        currentUser.lihatBarang();
                        break;
                    case 2:
                        currentUser.sewaBarang();
                        break;
                    case 3:
                        if (currentUser instanceof Admin) {
                            ((Admin) currentUser).addBarang();
                        }
                        break;
                    case 4:
                        if (currentUser instanceof Admin) {
                            ((Admin) currentUser).lihatDaftarPenyewa();
                        }
                        break;
                    case 5:
                        if (currentUser instanceof Admin) {
                            ((Admin) currentUser).hapusPenyewa();
                        }
                        break;
                    case 0:
                        System.out.println("Keluar dari sistem. Terima kasih!");
                        return;
                    default:
                        System.out.println("Pilihan tidak valid.");
                        break;
                }
            }
        }
    }
}
