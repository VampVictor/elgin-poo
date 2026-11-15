# 🖨️ Controle de Impressora Elgin i8 ou i9


---

## 📘 Sobre o Projeto

Este projeto foi desenvolvido para a disciplina de **Programação Orientada a Objetos**.
O sistema utiliza Java para se comunicar com uma impressora térmica **Elgin i9**, por meio da biblioteca `E1_Impressora01.dll` e do JNA.
A aplicação funciona pelo console, com login e um menu de operações de impressão e controle do equipamento.
O código utiliza classes, atributos, métodos, laços de repetição, validação de entradas e tratamento de erros.

---

## 🧰 Tecnologias Utilizadas

- **Java 21**.
- **IntelliJ IDEA**.
- **JNA 5.15.0**, para comunicação entre Java e a DLL.
- **DLL E1_Impressora01**, fornecida pelo professor.
- **Windows x64**, com JDK/JVM x64 para Intel/AMD.
- **Impressora Elgin i9**.

---

## 📁 Estrutura do Projeto

| Arquivo ou pasta | Responsabilidade |
|---|---|
| `src/Main.java` | Inicia o programa, realiza o login e apresenta o menu em laço. |
| `src/Usuario.java` | Guarda as credenciais e verifica a autenticação. |
| `src/Entrada.java` | Lê os dados digitados e valida as entradas do console. |
| `src/Impressora.java` | Guarda a configuração da conexão e executa as operações da impressora. |
| `src/ImpressoraDLL.java` | Interface JNA original fornecida pelo professor, mantida sem alterações. |
| `Bibliotecas E1 Impressora/x64/` | Contém a DLL utilizada pela JVM x64. |
| `Bibliotecas E1 Impressora/x86/` | Contém a DLL x86 original, preservada no projeto. |
| `libs Java/` | Contém o arquivo `jna-5.15.0.jar`. |
| `XMLSAT.xml` | XML de exemplo para impressão de venda SAT. |
| `CANC_SAT.xml` | XML de exemplo para impressão de cancelamento SAT. |
| `.gitignore` | Exclui arquivos temporários e configurações locais do versionamento. |
| `README.md` | Apresenta o projeto e as instruções de execução. |

As pastas das bibliotecas e os dois XMLs devem ficar na raiz do projeto. Esta implementação utiliza a DLL x64; a versão x86 foi preservada como recebida.

---

## ⚙️ Instalação e Execução

### 1. Baixar o projeto

1. Acesse o repositório do projeto no GitHub.
2. Clique em **Code → Download ZIP**.
3. Extraia o arquivo por completo.

### 2. Abrir no IntelliJ IDEA

1. Crie ou abra um projeto **Java** no IntelliJ IDEA, com build system **IntelliJ** e **JDK 21 x64**.
2. Coloque os cinco arquivos Java dentro de `src`, sem declaração `package`. O projeto utiliza classpath simples, sem `module-info.java`.
3. Em **File → Project Structure → Libraries → + → Java**, adicione `libs Java/jna-5.15.0.jar` e associe a biblioteca ao módulo do projeto.
4. Confirme o SDK do projeto e o escopo **Compile** da biblioteca nas dependências do módulo.
5. Configure os arquivos e o console como **UTF-8**.
6. Execute `Main.java` uma vez para criar a configuração de execução.
7. Em **Run → Edit Configurations**, confirme a classe principal `Main`, o JDK 21 x64 e o **Working directory** apontando para a raiz do projeto.
8. Nas opções da JVM, utilize `-Dfile.encoding=UTF-8 -Djna.encoding=UTF-8`.
9. Instale o driver da Elgin i9 correspondente à interface utilizada, conecte e ligue a impressora com papel. Feche outros programas que estejam usando a conexão.
10. Execute `Main.java`, faça login e utilize a opção **1** para configurar a conexão.
11. Para USB nativo, informe tipo `1`, modelo `i9`, conexão `USB` e parâmetro `0`.
12. Escolha a opção **2** para abrir a conexão. As operações físicas são liberadas quando a abertura retorna `0`.

---

## 🔐 Login

- **Usuário:** `admin`.
- **Senha:** `1234`.
- São permitidas **três tentativas**.
- A cada erro, o programa informa quantas tentativas restam.
- Após três tentativas incorretas, o programa é encerrado.
- Quando a autenticação é aceita, o menu é apresentado.

---

## 📋 Funcionalidades do Menu

| Opção | O que faz |
|---|---|
| 1 | Configura tipo de conexão, modelo, porta/endereço e parâmetro. |
| 2 | Abre a conexão com a impressora. |
| 3 | Fecha a conexão com a impressora. |
| 4 | Imprime texto com alinhamento, estilo, altura e quantidade de cópias. |
| 5 | Imprime QR Code com tamanho e nível de correção. |
| 6 | Imprime código de barras CODE 128 com 1 a 12 dígitos. |
| 7 | Imprime o XML de venda SAT fornecido como exemplo. |
| 8 | Imprime o XML de cancelamento SAT fornecido como exemplo, usando a assinatura correspondente do exercício. |
| 9 | Envia o comando de abertura de gaveta com pino e tempos informados. |
| 10 | Envia o comando de sinal sonoro, com quantidade de bips informada. |
| 11 | Avança o papel. |
| 12 | Consulta o status do papel. |
| 13 | Envia o comando de corte do papel. |
| 14 | Mostra sistema operacional, versão do Java, arquitetura da JVM, diretório de trabalho e configuração da conexão. |
| 0 | Encerra o programa e tenta fechar a conexão, caso esteja aberta. |

As opções **4 a 13** verificam a conexão antes de solicitar os dados da operação. O programa confere os retornos dos comandos, considera erro apenas retorno negativo em `ImpressaoTexto` e interpreta separadamente os retornos de status do papel.

---

## 🧪 Testes Realizados

| Teste | Situação |
|---|---|
| Compilação no computador do grupo | [RESULTADO] |
| Login correto e acesso ao menu | [RESULTADO] |
| Três tentativas incorretas e encerramento | [RESULTADO] |
| Digitação de letras e números fora da faixa | [RESULTADO] |
| Bloqueio das operações sem conexão, antes de pedir dados | [RESULTADO] |
| Carregamento da DLL no Windows | [RESULTADO] |
| Abertura e fechamento da conexão real | Pendente. |
| Impressão de texto, acentos e várias cópias | Pendente. |
| Impressão e leitura de QR Code | Pendente. |
| Impressão e leitura de CODE 128 | Pendente. |
| Impressão do XML SAT e do cancelamento SAT | Pendente. |
| Tratamento de arquivo XML inexistente com conexão aberta | Pendente. |
| Sinal sonoro | Pendente. |
| Abertura da gaveta | Pendente. |
| Avanço, status do papel e corte | Pendente. |

- **Data dos testes:** [DATA].
- **Modelo da impressora:** Elgin i9.
- **Windows:** [VERSÃO].
- **JDK:** [VERSÃO].
- **Driver:** [NOME E VERSÃO].
- **Conexão:** [TIPO].

---

## 📚 Documentação Adicional


- [Documentação do SDK E1_Impressora](https://elgindevelopercommunity.github.io/group___m1.html).
- [Códigos de retorno e erros da Elgin](https://elgindevelopercommunity.github.io/group__g1.html).
- [Download Center da Elgin](https://www.elgin.com.br/download-center).

---

## 👥 Autores

- Alonso Araújo
- Cauã Germano
- Victor Faria de Barros