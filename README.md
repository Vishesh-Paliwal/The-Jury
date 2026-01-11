# The Jury 🏛️

**A Kotlin Multiplatform AI-Powered Jury Deliberation System**

*My submission for the KotlinConf 2026 Kotlin Multiplatform Contest*

The Jury is an innovative Kotlin Multiplatform application that simulates intelligent jury deliberations using AI agents. Unlike traditional single-response AI systems, The Jury creates dynamic, multi-agent discussions where different AI personas engage in structured debates, ask follow-up questions, and reach collective verdicts through guided deliberation.

## 🎯 What Makes The Jury Special

**Multi-Agent AI Deliberation**: Instead of getting one AI response, watch multiple AI personas with distinct personalities and expertise engage in realistic jury-style discussions, complete with cross-examination and collaborative reasoning.

**Dual Execution Modes**: 
- **Parallel Mode**: Get quick individual responses from all agents simultaneously
- **Jury Mode**: Experience structured deliberations with moderator-guided discussions, follow-up questions, and final verdicts

**Persistent Trial History**: All deliberations are automatically saved with full transcripts, allowing you to revisit past cases and learn from previous discussions.

**Real-time Streaming**: Watch conversations unfold in real-time as agents think, respond, and build upon each other's arguments.

## 🎬 Demo

**📹 [Click to Watch the Demo Video](https://drive.google.com/file/d/1uH2yILTJ1oUJvQ7Greb7cUP_a7L7Y4LL/view?usp=sharing)**

**Mobile view Screnshot :**

<img width="356" height="758" alt="Screenshot 2026-01-11 at 6 46 10 PM" src="https://github.com/user-attachments/assets/3a06c9d0-1e4c-44ad-979b-a2ef5793527d" />


**Desktop view Screenshot :**

<img width="1470" height="920" alt="Screenshot 2026-01-11 at 6 43 44 PM" src="https://github.com/user-attachments/assets/7bcb5f51-2869-4488-bf8d-e8d7ef400879" />

<img width="1470" height="920" alt="Screenshot 2026-01-11 at 6 44 43 PM" src="https://github.com/user-attachments/assets/ef3d2501-af31-498c-b5f8-8a47dd61df1b" />


## 🚀 Key Features

### 🤖 Intelligent Agent System
- **Customizable AI Personas**: Create agents with distinct personalities, expertise, and reasoning styles
- **Dynamic Interactions**: Agents respond to each other's arguments and ask clarifying questions
- **Streaming Responses**: Real-time conversation flow with thinking indicators

### ⚖️ Jury Deliberation Engine
- **Structured Process**: Moderator-guided discussions following realistic jury procedures
- **Multi-Round Debates**: Initial responses, follow-up questions, and final deliberation
- **Verdict Generation**: Collaborative decision-making with reasoned conclusions

### 💾 Comprehensive Persistence
- **Trial History**: Automatic saving of all deliberations with full transcripts
- **SQLDelight Integration**: Robust local database storage across platforms
- **Session Restoration**: Resume interrupted trials and review past cases

### 🎨 Modern UI/UX
- **Material 3 Design**: Clean, accessible interface following modern design principles
- **Responsive Layout**: Adaptive UI that works seamlessly on mobile and desktop
- **Full-Screen Transcript**: Immersive reading experience for long deliberations
- **Real-time Updates**: Live conversation updates with smooth animations

## 🛠️ Technical Architecture

### Platforms Supported
- **Android** 
- **Desktop** 

### Core Technologies

| Technology | Purpose |
|------------|---------|
| **Kotlin Multiplatform** | Shared business logic across platforms |
| **Compose Multiplatform** | Modern declarative UI framework |
| **Koog AI Agents** | Advanced AI agent orchestration and streaming |
| **SQLDelight** | Type-safe database operations |
| **Koin** | Lightweight dependency injection |
| **Voyager** | Navigation and screen management |
| **Kotlinx Serialization** | JSON serialization for data persistence |
| **Kotlinx Coroutines** | Asynchronous programming and streaming |

### Architecture Highlights
- **Clean Architecture**: Separation of concerns with clear data/domain/presentation layers
- **Reactive Streams**: Real-time UI updates using Kotlin Flows
- **Dependency Injection**: Modular, testable codebase with Koin
- **Type Safety**: Compile-time guarantees with Kotlin's type system

## 📋 Prerequisites

Before running The Jury, ensure you have:

1. **Development Environment**:
   - IntelliJ Idea 
   - JDK 17
   - Kotlin Multiplatform plugin

2. **AI API Access**:
   - Gemini AI API KEY
   - other AI providers can be used , you just have to configure respective provider for koog in code

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/The-Jury.git
cd The-Jury
```

### 2. Configure AI API
Create a `local.properties` file in the project root:
```properties
API_KEY=your_gemni_ai_api_key_here
```

### 3. Build and Run

#### Android

just sync gradle and easily use default runner from IntelliJ Idea . composeApp with a phone of your choice

<img width="489" height="255" alt="Screenshot 2026-01-11 at 3 33 00 PM" src="https://github.com/user-attachments/assets/c49c38ce-0123-44da-af3c-a24966bd5136" />

<img width="653" height="46" alt="Screenshot 2026-01-11 at 3 30 28 PM" src="https://github.com/user-attachments/assets/95182118-680a-4738-92e5-ecaf490478de" />


#### Desktop (MAC PREFFERED) (JVM)

just sync gradle and easily use default runner from IntelliJ Idea . composeApp (jvm) for desktop

<img width="653" height="46" alt="Screenshot 2026-01-11 at 3 31 43 PM" src="https://github.com/user-attachments/assets/e1639d75-1bc7-41e4-9cea-47afa8334f4b" />


## 🎮 How to Use The Jury

### Creating Your First Trial

1. **Launch the App**: Open The Jury on your preferred platform
2. **Set Up Agents**: Navigate to the "Agents" tab to create AI personas with different expertise (lawyer, scientist, ethicist, etc.)
3. **Choose Mode**: Select between Parallel (quick responses) or Jury (structured deliberation)
4. **Ask Your Question**: Enter a complex question or scenario that benefits from multiple perspectives
5. **Watch the Deliberation**: Observe as agents discuss, debate, and reach conclusions

### Example Use Cases

- **Ethical Dilemmas**: "Should AI be allowed to make medical diagnoses without human oversight?"
- **Business Decisions**: "Should our company prioritize profit or environmental sustainability?"
- **Technical Debates**: "Is microservices architecture always better than monolithic design?"
- **Social Issues**: "How should social media platforms handle misinformation?"

### Advanced Features

- **Trial History**: Review past deliberations to track reasoning patterns
- **Agent Customization**: Fine-tune persona instructions for specialized expertise
- **Full-Screen Mode**: Immersive reading experience for complex discussions
- **Export Transcripts**: Save important deliberations for future reference

## 🏗️ Project Structure

```
The-Jury/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/kotlin/com/example/the_jury/
│   │   │   ├── model/          # Data models and entities
│   │   │   ├── service/        # Business logic and AI integration
│   │   │   ├── ui/            # Compose UI components and screens
│   │   │   ├── repo/          # Data repositories
│   │   │   └── di/            # Dependency injection modules
│   │   ├── androidMain/       # Android-specific implementations
│   │   ├── jvmMain/          # Desktop-specific implementations
│   │   └── commonMain/sqldelight/ # Database schema
│   └── build.gradle.kts
├── gradle/
└── README.md
```

## 🎯 Contest Compliance

This project fully complies with the KotlinConf 2026 Kotlin Multiplatform Contest requirements:

- ✅ **Kotlin Multiplatform**: Runs on Android and Desktop (JVM)
- ✅ **Original Creation**: Built specifically for this contest
- ✅ **Creative Showcase**: Demonstrates innovative AI agent orchestration
- ✅ **Not a Template**: Complex, feature-rich application
- ✅ **Not a Library**: Complete end-user application
- ✅ **AI Integration**: Advanced AI agent system with streaming capabilities
- ✅ **Detailed Documentation**: Comprehensive setup and usage instructions
- ✅ **English Language**: All content in English
- ✅ **GitHub Submission**: Available on GitHub with detailed README
- ✅ **Screencast Demo**: Video demonstration of main features (see Demo section above)

## 🚀 Future Enhancements

- **iOS Support**: Extend to iOS platform for complete mobile coverage
- **Web Platform**: Browser-based version using Kotlin/JS
- **Voice Integration**: Audio-based deliberations with speech synthesis
- **Advanced Analytics**: Sentiment analysis and argument mapping
- **Collaborative Features**: Multi-user jury sessions
- **Custom AI Models**: Integration with additional AI providers

## 🤝 Contributing

While this is a contest submission, feedback and suggestions are welcome! Please feel free to:

1. Open issues for bugs or feature requests
2. Submit pull requests for improvements
3. Share your interesting jury deliberations
4. Suggest new agent personas or use cases

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **JetBrains** for Kotlin Multiplatform and Compose Multiplatform
- **Koog AI** for the powerful agent orchestration framework
- **Kotlin Foundation** for organizing the contest and fostering innovation
- **Open Source Community** for the amazing libraries that made this possible

## 📞 Contact

For questions about this project or the contest submission:

- **GitHub**: Vishesh-Paliwal
- **Email**: vishesh.paliwal23bcs10013@sst.scaler.com

---

*Built with ❤️ using Kotlin Multiplatform for the KotlinConf 2026 Contest*
