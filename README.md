# ⚡ EV Charging Station Management System  

## 📌 Overview  
The **EV Charging Station Management System** is a Spring Boot web application designed to simplify the process of locating charging stations, booking charging slots, handling payments, and managing users with role-based access (Admin/User).  

This project demonstrates the use of **Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, and MySQL** for building a real-world application with full CRUD, booking, and admin management features.  

---

## 🚀 Features  

### 👩‍💻 User Features  
- Register/Login with role selection (User/Admin)  
- View available charging stations on Google Maps  
- Search & filter stations  
- Book charging slots  
- Make payments after booking  
- View booking and payment history  
- User dashboard with personalized data  

### 🛠️ Admin Features  
- Admin dashboard  
- Manage users (view, edit, delete, paginate user list)  
- Manage charging stations and slots (CRUD operations)  
- View all bookings and payments  
- Role-based access restrictions  

---

## 🏗️ Tech Stack  
- **Backend:** Spring Boot, Spring Security, Spring Data JPA  
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript  
- **Database:** MySQL  
- **Other Integrations:** Google Maps API (station display)  

---

## ⚙️ Installation & Setup  

1. **Clone the repository**  
```bash
git clone https://github.com/yourusername/ev-charging-station.git
cd ev-charging-station


2. Run the application
mvn spring-boot:run

3. Access the app

User Dashboard → http://localhost:8080/user/dashboard

Admin Dashboard → http://localhost:8080/admin/dashboard

🔑 Default Roles & Access

Admin → Can manage users, stations, slots, and bookings

User → Can search stations, book slots, and view history

📌 Future Enhancements

Mobile app integration

Online payment gateway (Razorpay/Stripe)

Real-time slot availability updates

Smart charging analytics (AI-based load balancing)

🤝 Contribution

Contributions are welcome!

Fork the repo

Create a feature branch (feature-xyz)

Commit changes

Open a Pull Request

📜 License

This project is licensed under the MIT License.
