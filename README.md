# BetterMe

BetterMe is a modern Android application that helps users build and maintain positive daily habits.  
The application is developed using Kotlin and follows Clean Architecture combined with the MVVM pattern to ensure scalability, maintainability, and clear separation of concerns.

The goal of BetterMe is to support users in improving their lifestyle through habit tracking, goal setting, and motivational progress monitoring.

---

## Project Overview

BetterMe focuses on:

- Building positive daily habits  
- Tracking progress over time  
- Providing motivation and structured habit management  
- Supporting long-term personal growth  

This project is designed with production-level architecture suitable for graduation projects and real-world applications.

---

## Technology Stack

### Language
- Kotlin

### UI
- Jetpack Compose  
- Custom Material Theme  

### Architecture
- Clean Architecture  
- MVVM (Model - View - ViewModel)

### Dependency Injection
- Koin

### Local Database
- Room

### Networking
- Retrofit

### Image Loading
- Coil

### Navigation
- Navigation Compose

### Language Support
- Vietnamese  
- Vietnamese + English  

---

## Architecture

The project follows Clean Architecture principles with three main layers:

### 1. Presentation Layer
- Jetpack Compose UI  
- ViewModel (MVVM)  
- Navigation  
- UI State management  
- StateFlow / LiveData  

### 2. Domain Layer
- Business logic  
- Use cases  
- Repository interfaces  
- Domain models  

### 3. Data Layer
- Room database  
- Retrofit API services  
- Repository implementations  
- DTO and entity mapping  

This separation ensures:

- High testability  
- Clear business logic isolation  
- Easy scalability  
- Long-term maintainability  

---

## MVVM Pattern

The application uses MVVM architecture to maintain a clear separation between UI and business logic.

Each screen contains:

- View: Jetpack Compose UI  
- ViewModel: Handles state and business interaction  
- Model: Data models and domain entities  

### Application Flow

User Action  
→ View  
→ ViewModel  
→ UseCase  
→ Repository  
→ Update State  
→ UI Recomposition  

---

## Features

### Core Features
- Create and manage habits  
- Daily habit tracking  
- Streak tracking  
- Progress statistics  
- Local data persistence with Room  

### Extended Features
- Reminder system  
- Motivation quotes  
- Habit categories  
- Analytics and charts  
- Sync with remote API (optional future)  

---

## Folder Structure
data/
local/
remote/
repository/

domain/
model/
repository/
usecase/

presentation/
ui/
navigation/
theme/
viewmodel/

di/
Koin modules


---

## Setup Instructions

Clone the repository:
git clone https://github.com/your-username/BetterMe.git

Open the project in Android Studio  
Sync Gradle  
Run on emulator or real device  

Minimum SDK: 24+

---

## Design Principles

- Single Responsibility Principle  
- Separation of Concerns  
- Unidirectional Data Flow  
- Immutable UI State  
- Dependency Injection  

---

## Future Improvements

- Cloud synchronization  
- User authentication  
- AI-powered habit suggestions  
- Smart analytics  
- Multi-device sync  

---

## License

This project is developed for educational and personal development purposes.

---

## Author

**Tran Hai Dang**  
Email: trandang211@gmail.com
