# Contributing to SurvLink

Thank you for your interest in contributing to SurvLink! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Submitting Changes](#submitting-changes)
- [Adding New Instrument Support](#adding-new-instrument-support)

## Code of Conduct

This project follows a simple code of conduct:
- Be respectful and constructive
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/blue_tooth.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test thoroughly
6. Submit a pull request

## Development Setup

### Requirements

- **JDK 17** or higher
- **Android SDK** with API 35
- **Android Studio** (recommended) or command-line tools
- **Git** for version control

### Environment Setup

```bash
# Set Java home
export JAVA_HOME=/path/to/jdk-17

# Set Android SDK root
export ANDROID_SDK_ROOT=/path/to/android-sdk

# Verify setup
./gradlew --version
```

### Building the Project

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run specific test class
./gradlew :app:testDebugUnitTest --tests 'com.unforgettable.bluetoothcollector.data.protocol.GeoComClientTest'
```

## Project Structure

```
app/src/main/java/com/unforgettable/bluetoothcollector/
├── data/
│   ├── bluetooth/       # Bluetooth connection management
│   ├── export/          # CSV/TXT export writers
│   ├── protocol/        # Protocol abstraction layer
│   ├── storage/         # Room database
│   └── instrument/      # Instrument catalog
├── domain/model/        # Domain models
├── ui/collector/        # UI components and ViewModel
└── MainActivity.kt

app/src/test/java/       # Unit tests (mirror main structure)
```

## Coding Standards

### Kotlin Style Guide

Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names
- Prefer `val` over `var` when possible
- Use trailing commas in multi-line declarations

### Architecture Patterns

- **MVVM**: ViewModel manages state, UI observes StateFlow
- **Single Activity**: Use Compose for UI, avoid fragments
- **Repository Pattern**: Separate data sources from business logic
- **Dependency Injection**: Constructor injection, avoid singletons

### Code Organization

```kotlin
// Good: Clear separation of concerns
class GeoComClient(
    private val transport: ProtocolTransport
) {
    private val commandMutex = Mutex()
    
    suspend fun sendCommand(command: GeoComCommand): GeoComResponse? {
        // Implementation
    }
}

// Bad: God class with multiple responsibilities
class BluetoothManager {
    fun connect() { }
    fun disconnect() { }
    fun sendCommand() { }
    fun parseResponse() { }
    fun exportCsv() { }  // Wrong layer!
}
```

## Testing Requirements

### Minimum Coverage: 80%

All new features must include:

1. **Unit Tests**: Test individual functions and classes
2. **Integration Tests**: Test component interactions
3. **UI Tests**: Test Compose components (when applicable)

### Test Structure

```kotlin
class GeoComClientTest {
    private lateinit var mockTransport: ProtocolTransport
    private lateinit var client: GeoComClient
    
    @Before
    fun setup() {
        mockTransport = mock()
        client = GeoComClient(mockTransport)
    }
    
    @Test
    fun `sendCommand should return parsed response on success`() {
        // Arrange
        val expectedResponse = "%R1P,0,0:0,1.234,0.567,100.5\r\n"
        whenever(mockTransport.sendBytes(any())).thenReturn(Unit)
        whenever(mockTransport.readUntilComplete()).thenReturn(expectedResponse)
        
        // Act
        val result = runBlocking { client.sendCommand(GeoComCommand.GetSimpleMeasurement) }
        
        // Assert
        assertNotNull(result)
        assertEquals(0, result?.returnCode)
    }
}
```

### Running Tests

```bash
# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run with coverage report
./gradlew :app:testDebugUnitTest :app:jacocoTestReport

# Run specific test
./gradlew :app:testDebugUnitTest --tests '*GeoComClientTest'
```

## Submitting Changes

### Pull Request Process

1. **Update documentation**: README, CHANGELOG, code comments
2. **Add tests**: Ensure 80%+ coverage for new code
3. **Run all tests**: `./gradlew :app:testDebugUnitTest`
4. **Build successfully**: `./gradlew :app:assembleDebug`
5. **Update CHANGELOG.md**: Add entry under "Unreleased"
6. **Create PR**: Use descriptive title and detailed description

### PR Title Format

```
feat: add GeoCOM protocol support for TS60
fix: resolve Bluetooth disconnect detection issue
docs: update README with new instrument support
test: add unit tests for GeoComClient
refactor: extract protocol abstraction layer
```

### PR Description Template

```markdown
## Description
Brief description of what this PR does.

## Motivation
Why is this change needed? What problem does it solve?

## Changes
- List of specific changes
- Another change
- Yet another change

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed
- [ ] All tests passing

## Screenshots (if applicable)
Add screenshots for UI changes.

## Checklist
- [ ] Code follows project style guidelines
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Tests added with 80%+ coverage
- [ ] All tests passing
- [ ] No breaking changes (or documented if unavoidable)
```

## Adding New Instrument Support

### Step 1: Add to Instrument Catalog

Edit `app/src/main/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalog.kt`:

```kotlin
InstrumentModel(
    modelId = "NEW_MODEL",
    brandId = "brand_name",
    displayName = "New Model Name",
    delimiterStrategy = DelimiterStrategy.LINE_DELIMITED,
    firmwareFamily = "FamilyName"  // "Captivate" for GeoCOM, null for passive
)
```

### Step 2: Implement Protocol (if needed)

If the instrument uses a new protocol:

1. Create protocol handler implementing `ProtocolHandler`
2. Add to `DefaultProtocolHandlerFactory`
3. Write unit tests

### Step 3: Add Export Format (if needed)

If the instrument needs custom export:

1. Create export writer implementing `ExportWriter`
2. Add to export selection logic
3. Write unit tests

### Step 4: Update Documentation

- Add to README.md supported instruments table
- Update CHANGELOG.md
- Add usage instructions if protocol differs

### Step 5: Test

- Unit tests for new protocol/export logic
- Manual testing with real hardware (if available)
- Document testing results in PR

## Questions?

- Open an issue for bugs or feature requests
- Start a discussion for questions or ideas
- Check existing issues before creating new ones

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
