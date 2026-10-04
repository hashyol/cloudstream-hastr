# SezonlukDizi CloudStream Eklenti Deposu

Bu depo, sadece [sezonlukdizi.cc](https://sezonlukdizi.cc/) sitesini içeren kişisel CloudStream sağlayıcı (provider) deposudur.

---

## 📱 CloudStream'e Ekleme

GitHub deponuzu oluşturup kodları yükledikten sonra CloudStream uygulamasında şu adımları izleyin:

1. CloudStream uygulamasını açın.
2. **Ayarlar (Settings) > Eklentiler (Extensions)** sayfasına gidin.
3. **Depo Ekle (Add Repository)** butonuna tıklayın.
4. Aşağıdaki bilgileri girin:
   - **Depo Adı:** `hashyol`
   - **Depo URL:** `https://cdn.jsdelivr.net/gh/hashyol/cloudstream-hastr@master/repo.json`
5. Kaydettikten sonra açılan listeden **SezonlukDizi** eklentisini indirip kurun.

---

## 🚀 GitHub'a Yükleme ve Otomasyon Adımları

Yeni oluşturduğunuz `hashyol` hesabında (`https://github.com/hashyol/cloudstream-hastr`):

### 1. GitHub Actions İzinlerini Ayarlayın
Deponuzun GitHub sayfasında:
1. **Settings > Actions > General** sekmesine gidin.
2. **Workflow permissions** bölümünde **"Read and write permissions"** seçeneğini işaretleyip kaydedin. *(Bu, GitHub Actions'ın derlenen eklentileri `builds` dalına otomatik yükleyebilmesi için gereklidir)*.

### 2. Kodları Depoya Gönderin (Push)
Terminalde bu klasörde şu komutları çalıştırarak kodları yeni hesabınıza gönderebilirsiniz:

```bash
git init
git add .
git commit -m "SezonlukDizi Cloudstream eklentisi ilk sürüm"
git branch -M master
git remote add origin https://github.com/hashyol/cloudstream-hastr.git
git push -u origin master
```

Push işleminden sonra GitHub Actions otomatik olarak:
- Eklentiyi derler (`SezonlukDizi.cs3`).
- `plugins.json` listesini üretir.
- Sonuçları `builds` isimli dala (branch) otomatik yükler.

---

## 💻 Yerel (Local) Derleme ve Doğrudan Yükleme

GitHub kullanmadan doğrudan bilgisayarınızdan telefonunuza yüklemek isterseniz:

### Eklentiyi Derlemek İçin:
```bash
./gradlew make
```
*Derlenen eklenti paketi:* `SezonlukDizi/build/SezonlukDizi.cs3`

### ADB ile Doğrudan Telefona Yüklemek İçin:
Telefonunuz USB hata ayıklama ile bağlıyken:
```bash
./gradlew SezonlukDizi:deployWithAdb
```
*(Veya üretilen `.cs3` dosyasını telefonunuza gönderip CloudStream ile açarak da doğrudan yükleyebilirsiniz).*

---

## ⚖️ Yasal Uyarı ve Sorumluluk Reddi (Disclaimer)

### Türkçe
- **İçerik Barındırmama:** Bu depo, eklenti ve geliştirici; hiçbir video, ses, görsel veya medya dosyasını kendi sunucularında barındırmaz, kaydetmez, yüklemez ya da yeniden dağıtmaz.
- **Ayrıştırma (Scraping) Niteliği:** Bu yazılım, internet üzerinde herkese açık olarak yayınlanan web sayfalarını (`sezonlukdizi.cc`) bir web tarayıcısı gibi ziyaret ederek HTML içeriğini ayrıştıran (web scraping) bir araçtan ibarettir.
- **Bağlantısızlık:** Geliştiricinin adı geçen internet sitesiyle, içerik sağlayıcılarıyla veya video sunucularıyla hiçbir bağı, ortaklığı, sponsorluğu veya kontrolü bulunmamaktadır.
- **Telif Hakları:** Telif hakkına konu olan içeriklerle ilgili tüm hak talepleri ve bildirimler, ilgili içeriği fiilen barındıran üçüncü taraf dosya barındırma servislerine (file host) ve kaynak internet sitelerine iletilmelidir.
- **Kişisel ve Eğitim Amaçlı Kullanım:** Bu proje tamamen eğitim, araştırma ve kişisel test amaçlarıyla hazırlanmıştır. Yazılımın kullanımından doğabilecek her türlü hukuki sorumluluk münhasıran son kullanıcıya aittir.

### English
- **No Content Hosting:** This repository, extension, and developer do not host, store, stream, or re-transmit any media files, copyrighted videos, or content on any server.
- **Scraper / Parser Nature:** This software merely functions as an open-source parser/client that reads publicly accessible HTML from third-party websites (`sezonlukdizi.cc`), similar to a web browser.
- **Non-Affiliation:** The developer is not affiliated with, endorsed by, or connected to the operators of `sezonlukdizi.cc` or any external streaming providers.
- **Copyright Inquiries:** For any copyright infringement concerns, inquiries must be directed to the actual file-hosting services and original content distributors.
- **Educational & Personal Use Only:** This software is provided strictly for educational, experimental, and personal testing purposes. The end user assumes all responsibilities arising from its use.

