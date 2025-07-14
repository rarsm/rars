# RARSM - RISC-V Assembler and Runtime Simulator (iMproved)

RARSM is a fork of [RARS](https://github.com/TheThirdOne/rars), a lightweight IDE for RISC-V assembly language programming. This project enhances the original RARS with additional features and improvements.

## Features
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

## Installation

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
