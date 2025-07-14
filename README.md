# RARSM - RISC-V Assembler and Runtime Simulator (iMproved)

RARSM is a fork of [RARS](https://github.com/TheThirdOne/rars), a lightweight IDE for RISC-V assembly language programming. This project enhances the original RARS with additional features and improvements.

## What It Is and Relation to RARS
RARSM builds upon the foundation of RARS, adding new features and addressing limitations. While RARS provides a robust environment for RISC-V assembly programming, RARSM introduces enhancements to improve usability and functionality.

## Features and Screenshots
RARSM includes the following features:
- Enhanced functionality over the original RARS.
- Improvements include:
  - [Pull Request #190](https://github.com/TheThirdOne/rars/pull/190)
  - [Pull Request #191](https://github.com/TheThirdOne/rars/pull/191)
  - [Pull Request #192](https://github.com/TheThirdOne/rars/pull/192)
  - [Pull Request #193](https://github.com/TheThirdOne/rars/pull/193)
  - [Pull Request #194](https://github.com/TheThirdOne/rars/pull/194)
  - [Pull Request #195](https://github.com/TheThirdOne/rars/pull/195)
  - [Pull Request #196](https://github.com/TheThirdOne/rars/pull/196)
  - [Pull Request #197](https://github.com/TheThirdOne/rars/pull/197)

### Screenshots
Below are some screenshots showcasing the RARSM interface and features:

*(Add screenshots here)*

## How to Install

1. Clone the repository:
   ```bash
   git clone https://github.com/ElTitoDG/rars.git
   cd rars
   ```

2. Build the project using Maven:
   ```bash
   mvn clean package
   ```

3. Run the application:
   ```bash
   java -jar target/rars.jar
   ```

4. Optionally, generate an installable package for your operating system:
   - For macOS, Windows, or Linux, use the appropriate packaging tools to create an installer from the built JAR file.

## Help
RARSM includes comprehensive help documentation. Refer to the `src/help/` directory for detailed guides on:
- Debugging
- System calls
- Tools
- And more...

## How to Build
To build RARSM from source:
1. Ensure you have Java and Maven installed.
2. Run the following command in the project root:
   ```bash
   mvn clean package
   ```
3. The built JAR file will be located in the `target/` directory.

## Usage
RARSM provides a graphical interface for assembling and simulating RISC-V programs. You can:
- Write RISC-V assembly code.
- Assemble the code to check for syntax errors.
- Simulate the execution of the code.

## Examples
The `examples/` directory contains sample RISC-V assembly programs, such as:
- `bottles.s`
- `cat.s`
- `mastermind.s`
- `printf.s`

## License
RARSM is distributed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Contributing
Contributions are welcome! Feel free to fork the repository and submit pull requests.

## Acknowledgments
This project is based on the original [RARS](https://github.com/TheThirdOne/rars) by TheThirdOne.
