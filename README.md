# 🖨️ Controle de Impressora Elgin i9


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
| `testes/TestesSemHardware.java` | Verifica login, menu, parâmetros padrão, sequência de impressão/corte e tratamento de falhas com biblioteca simulada. |
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
| 4 | Pede apenas o texto, imprime uma cópia em formato normal e corta o papel. |
| 5 | Pede apenas o conteúdo do QR Code, imprime com parâmetros padrão e corta o papel. |
| 6 | Imprime automaticamente o código de barras CODE 128 de exemplo (`012345678912`) e corta o papel. |
| 7 | Pede o arquivo XML de venda SAT, imprime e corta; Enter usa `XMLSAT.xml`. |
| 8 | Pede o arquivo XML de cancelamento SAT, imprime com a assinatura do exemplo e corta; Enter usa `CANC_SAT.xml`. |
| 9 | Envia o comando de abertura de gaveta usando pino e tempos predefinidos. |
| 10 | Emite dois bips, sem solicitar parâmetros. |
| 11 | Avança três linhas, sem solicitar parâmetros. |
| 12 | Consulta o status do papel. |
| 13 | Envia um corte manual com avanço predefinido, sem solicitar parâmetros. |
| 14 | Mostra sistema operacional, versão do Java, arquitetura da JVM, diretório de trabalho e configuração da conexão. |
| 0 | Encerra o programa e tenta fechar a conexão, caso esteja aberta. |

As opções **4 a 13** verificam a conexão antes de solicitar os dados da operação. O programa confere os retornos dos comandos, considera erro apenas retorno negativo em `ImpressaoTexto` e interpreta separadamente os retornos de status do papel.

### Parâmetros padrão e corte automático

O usuário informa o conteúdo que deseja imprimir. Os detalhes técnicos ficam definidos na classe `Impressora`, sem perguntas a cada comando:

| Operação | Padrão utilizado |
|---|---|
| Texto | Alinhamento à esquerda, estilo normal, tamanho normal e uma cópia. |
| QR Code | Tamanho `4` e nível de correção `2` (15%). |
| CODE 128 | Tipo `8`, altura `100`, largura `2` e texto legível abaixo do código (`HRI = 2`). |
| Finalização de cada impressão | `AvancaPapel(3)` seguido de `Corte(5)`. |
| Gaveta | Pino `0`, tempo inicial `50` e tempo final `50`. |
| Sinal sonoro | Dois bips, mantendo os tempos `50` e `5` do exercício. |
| Avanço manual | Três linhas. |
| Corte manual | `Corte(5)`, uma única vez. |

O corte automático acontece nas cinco opções que imprimem: **4, 5, 6, 7 e 8**. Os métodos de texto com parâmetros continuam disponíveis no código; se forem usadas várias cópias, cada cópia avança e corta individualmente. Gaveta, sinal sonoro, avanço manual, consultas, conexão e diagnóstico não acrescentam cortes automáticos.

A finalização só ocorre depois de a DLL aceitar a impressão. Se a impressão falhar, não avança nem corta. Se o avanço falhar, não corta. Se o corte falhar, o programa avisa que o conteúdo já foi enviado, retorna ao menu e não reimprime automaticamente. Dados inválidos e XMLs inexistentes ou malformados também não disparam corte.

Os valores de pino e tempos da gaveta precisam corresponder ao acessório instalado; não são universais. Se necessário, ajuste as constantes `GAVETA_PINO`, `GAVETA_TEMPO_INICIO` e `GAVETA_TEMPO_FIM` uma vez na classe `Impressora`. A configuração da conexão continua solicitando tipo, modelo, porta/endereço e parâmetro, conforme o enunciado; não se deve adivinhar porta COM, velocidade ou endereço de rede.

---

## 🧪 Testes Realizados

O grupo informou que a versão anterior funcionou na impressora da escola. A alteração de corte automático e menu simplificado passou em **115 verificações sem hardware**, com biblioteca simulada. Esses testes verificam a lógica Java e os argumentos enviados; a DLL Windows não é executada na simulação. **A nova versão ainda precisa de teste físico na Elgin i9**, incluindo posição do corte e leitura dos códigos.

Para executar os testes sem hardware no terminal PowerShell, a partir da raiz do projeto, com JDK 21 e `javac` disponível:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -encoding UTF-8 -cp "libs Java/jna-5.15.0.jar" -sourcepath src -d build testes/TestesSemHardware.java
java -cp "build;libs Java/jna-5.15.0.jar" TestesSemHardware
```

O resultado esperado é `APROVADO: 115 verificações da lógica Java, sem hardware.` O log é salvo em `build/saida-testes.txt`. Os registros abaixo são os testes documentados originalmente, antes da revisão:

| Teste | Situação |
|---|---|
| Compilação no computador do grupo | Aprovado. |
| Login correto e acesso ao menu | Aprovado. |
| Três tentativas incorretas e encerramento | Aprovado. |
| Digitação de letras e números fora da faixa | Aprovado. |
| Bloqueio das operações sem conexão, antes de pedir dados | Aprovado. |
| Carregamento da DLL no Windows | Aprovado. |
| Abertura e fechamento da conexão real | Aprovado. |
| Impressão de texto, acentos e várias cópias | Aprovado. |
| Impressão e leitura de QR Code | Aprovado. |
| Impressão e leitura de CODE 128 | Aprovado. |
| Impressão do XML SAT e do cancelamento SAT | Aprovado. |
| Tratamento de arquivo XML inexistente com conexão aberta | Aprovado. |
| Sinal sonoro | Aprovado. |
| Abertura da gaveta | Aprovado. |
| Avanço, status do papel e corte | Aprovado. |

- **Data dos testes em casa:** 13/11/2026.
- **Windows:** Windows 11.
- **JDK:** 21.0.2, 64 bits.
- **Observação:** a DLL carregou, mas a tentativa de abertura USB sem impressora retornou -9999. A comunicação física será verificada na escola.

---

## 📚 Documentação Adicional


- [Documentação do SDK E1_Impressora](https://elgindevelopercommunity.github.io/group___m1.html).
- [Códigos de retorno e erros da Elgin](https://elgindevelopercommunity.github.io/group__g1.html).
- [Download Center da Elgin](https://github.com/ElginDeveloperCommunity/Impressoras/tree/master/Impressoras%20N%C3%A3o%20Fiscais).

---

## 👥 Autores

- Alonso Araújo
- Cauã Silva Germano
- Victor Faria de Barros
