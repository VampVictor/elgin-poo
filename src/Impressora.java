import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.IntSupplier;

/** Dados e operações da conexão. A interface ImpressoraDLL original não foi alterada. */
public class Impressora {
    // Parâmetros técnicos definidos uma vez, sem perguntas a cada operação.
    private static final int TEXTO_POSICAO = 0;
    private static final int TEXTO_ESTILO = 0;
    private static final int TEXTO_TAMANHO = 0;
    private static final int TEXTO_COPIAS = 1;
    private static final int QR_TAMANHO = 4;
    private static final int QR_CORRECAO = 2;
    private static final int LINHAS_APOS_IMPRESSAO = 3;
    private static final int AVANCO_CORTE = 5;
    // Ajustar uma vez conforme a gaveta instalada; não são universais.
    private static final int GAVETA_PINO = 0;
    private static final int GAVETA_TEMPO_INICIO = 50;
    private static final int GAVETA_TEMPO_FIM = 50;
    private static final int QUANTIDADE_BIPS = 2;

    // Valor copiado do TXT recebido e comparado com o CANC_SAT.xml fornecido.
    public static final String ASS_QRCODE =
            "Q5DLkpdRijIRGY6YSSNsTWK1TztHL1vD0V1Jc4spo/CEUqICEb9SFy82ym8EhBRZjbh3btsZhF+sjHqEMR159i4agru9x6KsepK/q0"
                    + "E2e5xlU5cv3m1woYfgHyOkWDNcSdMsS6bBh2Bpq6s89yJ9Q6qh/J8YHi306ce9Tqb/drKvN2XdE5noRSS32TAWuaQEVd7u+TrvXlOQ"
                    + "sE3fHR1D5f1saUwQLPSdIv01NF6Ny7jZwjCwv1uNDgGZONJdlTJ6p0ccqnZvuE70aHOI09elpjEO6Cd+orI7XHHrFCwhFhAcbalc+Z"
                    + "fO5b/+vkyAHS6CYVFCDtYR9Hi5qgdk31v23w==";

    private int tipo;
    private String modelo;
    private String conexao;
    private int parametro;
    private boolean configurada;
    private boolean conexaoAberta;
    private boolean falhaCarga;
    private ImpressoraDLL dll;

    public Impressora() {
        // A DLL só é carregada quando a pessoa escolhe abrir a conexão.
    }

    // Usado exclusivamente pelos testes sem hardware; não aparece no menu real.
    Impressora(ImpressoraDLL bibliotecaDeTeste) {
        dll = bibliotecaDeTeste;
    }

    public boolean isConexaoAberta() {
        return conexaoAberta;
    }

    public boolean exigirConexao() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes.");
            return false;
        }
        return true;
    }

    public void configurar(int tipo, String modelo, String conexao, int parametro) {
        if (conexaoAberta) {
            throw new IllegalStateException("Feche a conexão antes de reconfigurar.");
        }
        intervalo(tipo, 1, 4, "Tipo");
        if (modelo == null || !(modelo.equalsIgnoreCase("i8") || modelo.equalsIgnoreCase("i9"))) {
            throw new IllegalArgumentException("Modelo deve ser i8 ou i9.");
        }
        if (conexao == null || conexao.isBlank() || conexao.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Porta/endereço inválido.");
        }
        if (tipo == 1 && (!conexao.equalsIgnoreCase("USB") || parametro != 0)) {
            throw new IllegalArgumentException("USB usa conexão USB e parâmetro 0.");
        }
        if (tipo == 2) {
            if (!conexao.toUpperCase(Locale.ROOT).matches("COM[1-9][0-9]*")) {
                throw new IllegalArgumentException("Serial usa a porta real, como COM3.");
            }
            intervalo(parametro, 1, Integer.MAX_VALUE, "Baudrate");
        }
        if (tipo == 3) {
            intervalo(parametro, 1, 65535, "Porta TCP");
        }
        if (tipo == 4 && (!conexao.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}") || parametro != 0)) {
            throw new IllegalArgumentException("Bluetooth usa MAC AA:BB:CC:DD:EE:FF e parâmetro 0.");
        }
        this.tipo = tipo;
        this.modelo = modelo.toLowerCase(Locale.ROOT);
        this.conexao = (tipo == 1 || tipo == 2) ? conexao.toUpperCase(Locale.ROOT) : conexao;
        this.parametro = parametro;
        configurada = true;
        System.out.println("Configuração salva: " + resumo());
    }

    public String resumo() {
        if (!configurada) {
            return "Não configurada";
        }
        return "tipo=" + tipo + ", modelo=" + modelo + ", conexão=" + conexao
                + ", parâmetro=" + parametro;
    }

    private boolean carregarDll() {
        if (dll != null) {
            return true;
        }
        if (falhaCarga) {
            System.out.println("A DLL falhou nesta execução. Corrija o ambiente e reinicie o programa.");
            return false;
        }
        String sistema = System.getProperty("os.name", "");
        String arquitetura = System.getProperty("os.arch", "");
        if (!sistema.startsWith("Windows")
                || !(arquitetura.equalsIgnoreCase("amd64") || arquitetura.equalsIgnoreCase("x86_64"))) {
            System.out.println("Este projeto usa as DLLs fornecidas: execute com Windows e JDK x64 (Intel/AMD).");
            System.out.println("Ambiente atual: " + sistema + " / " + arquitetura);
            return false;
        }
        Path caminho = Path.of("Bibliotecas E1 Impressora", "x64", "E1_Impressora01.dll").toAbsolutePath();
        if (!Files.isRegularFile(caminho)) {
            System.out.println("DLL não encontrada: " + caminho);
            System.out.println("Confira o Working directory: deve ser a raiz do projeto.");
            return false;
        }
        try {
            // Pode ser sobrescrito em VM options, antes da primeira carga, se a
            // homologação do equipamento exigir outra codificação.
            if (System.getProperty("jna.encoding") == null) {
                System.setProperty("jna.encoding", "UTF-8");
            }
            dll = ImpressoraDLL.INSTANCE;
            return true;
        } catch (LinkageError e) {
            // Inclui UnsatisfiedLinkError e NoClassDefFoundError em nova tentativa.
            falhaCarga = true;
            System.out.println("Não foi possível carregar a DLL/JNA: " + e.getMessage());
            System.out.println("Confira JNA, JDK x64 e os arquivos originais. Após corrigir, reinicie.");
            return false;
        }
    }

    public boolean abrirConexao() {
        if (conexaoAberta) {
            System.out.println("A conexão já está aberta.");
            return false;
        }
        if (!configurada) {
            System.out.println("Use primeiro a opção 1 para configurar a conexão.");
            return false;
        }
        if (!carregarDll()) {
            return false;
        }
        try {
            int retorno = dll.AbreConexaoImpressora(tipo, modelo, conexao, parametro);
            if (retorno == 0) {
                conexaoAberta = true;
                System.out.println("Conexão aberta. Retorno: 0.");
                return true;
            }
            erro("Abrir conexão", retorno);
        } catch (LinkageError e) {
            falhaNativa(e);
        }
        return false;
    }

    public boolean fecharConexao() {
        if (!conexaoAberta) {
            System.out.println("A conexão já está fechada.");
            return false;
        }
        try {
            int retorno = dll.FechaConexaoImpressora();
            if (retorno == 0) {
                conexaoAberta = false;
                System.out.println("Conexão fechada. Retorno: 0.");
                return true;
            }
            erro("Fechar conexão", retorno);
        } catch (LinkageError e) {
            falhaNativa(e);
        }
        return false;
    }

    /** Regra comum às operações cujo sucesso é exatamente zero. */
    private boolean executarZero(String operacao, IntSupplier comando) {
        if (!exigirConexao()) {
            return false;
        }
        try {
            int retorno = comando.getAsInt();
            if (retorno == 0) {
                System.out.println(operacao + ": comando enviado. Retorno: 0.");
                return true;
            }
            erro(operacao, retorno);
        } catch (LinkageError e) {
            falhaNativa(e);
        }
        return false;
    }

    public boolean imprimirTexto(String texto) {
        return imprimirTexto(texto, TEXTO_POSICAO, TEXTO_ESTILO, TEXTO_TAMANHO, TEXTO_COPIAS);
    }

    public boolean imprimirTexto(String texto, int posicao, int estilo, int tamanho, int copias) {
        if (!exigirConexao()) {
            return false;
        }
        textoImprimivel(texto, 500, "Texto");
        intervalo(posicao, 0, 2, "Posição");
        intervalo(estilo, 0, 15, "Estilo");
        // 0..5 funciona no limite de altura dos dois modelos. Largura normal.
        intervalo(tamanho, 0, 5, "Tamanho");
        intervalo(copias, 1, 10, "Cópias");
        for (int copia = 1; copia <= copias; copia++) {
            try {
                int retorno = dll.ImpressaoTexto(texto + "\n", posicao, estilo, tamanho);
                if (retorno < 0) { // Neste comando, retorno positivo é normal.
                    erro("Texto, cópia " + copia, retorno);
                    return false;
                }
                System.out.println("Texto, cópia " + copia + ": retorno " + retorno + " (dados enviados).");
            } catch (LinkageError e) {
                falhaNativa(e);
                return false;
            }
            if (!finalizarImpressao()) {
                return false;
            }
        }
        return true;
    }

    public boolean imprimirQrCode(String dados) {
        return imprimirQrCode(dados, QR_TAMANHO, QR_CORRECAO);
    }

    public boolean imprimirQrCode(String dados, int tamanho, int nivel) {
        if (!exigirConexao()) {
            return false;
        }
        // Limite conservador da aplicação; ainda é preciso verificar se cabe no papel.
        textoImprimivel(dados, 200, "QR Code");
        intervalo(tamanho, 1, 6, "Tamanho do QR Code");
        intervalo(nivel, 1, 4, "Nível de correção");
        return executarZero("QR Code", () -> dll.ImpressaoQRCode(dados, tamanho, nivel))
                && finalizarImpressao();
    }

    public boolean imprimirCodigoBarras(String numeros) {
        if (!exigirConexao()) {
            return false;
        }
        if (numeros == null || !numeros.matches("[0-9]{1,12}")) {
            throw new IllegalArgumentException("Informe de 1 a 12 dígitos para o CODE 128.");
        }
        return executarZero("CODE 128", () -> dll.ImpressaoCodigoBarras(8, "{A" + numeros, 100, 2, 2))
                && finalizarImpressao();
    }

    public boolean imprimirXmlSat(String arquivo) throws IOException, SAXException, ParserConfigurationException {
        if (!exigirConexao()) {
            return false;
        }
        String xml = lerXml(arquivo);
        validarXml(xml, "CFe");
        // Envia o conteúdo, e não apenas o nome do arquivo, conforme o enunciado.
        return executarZero("XML SAT", () -> dll.ImprimeXMLSAT(xml, 0))
                && finalizarImpressao();
    }

    public boolean imprimirXmlCancelamento(String arquivo)
            throws IOException, SAXException, ParserConfigurationException {
        if (!exigirConexao()) {
            return false;
        }
        String xml = lerXml(arquivo);
        Document documento = validarXml(xml, "CFeCanc");
        Element inf = (Element) documento.getElementsByTagName("infCFe").item(0);
        if (inf == null || !"CFe13180314200166000166599000108160001324252883".equals(inf.getAttribute("Id"))
                || !"CFe13180314200166000166599000108160001316693175".equals(inf.getAttribute("chCanc"))) {
            throw new IllegalArgumentException("A assinatura constante pertence ao exemplo CANC_SAT.xml fornecido."
                    + " Para outro cancelamento, adapte a assinatura correspondente antes de usar.");
        }
        return executarZero("XML de cancelamento SAT", () -> dll.ImprimeXMLCancelamentoSAT(xml, ASS_QRCODE, 0))
                && finalizarImpressao();
    }

    /** Só finaliza após impressão aceita; uma falha interrompe a sequência. */
    private boolean finalizarImpressao() {
        if (!avancarPapel(LINHAS_APOS_IMPRESSAO)) {
            System.out.println("Conteúdo já enviado, mas o avanço falhou. Confira o papel antes de repetir.");
            return false;
        }
        if (!cortarPapel()) {
            System.out.println("Conteúdo já enviado, mas o corte falhou. Confira o papel; não reimprima automaticamente.");
            return false;
        }
        System.out.println("Impressão e corte: comandos enviados.");
        return true;
    }

    public boolean abrirGaveta() {
        return abrirGaveta(GAVETA_PINO, GAVETA_TEMPO_INICIO, GAVETA_TEMPO_FIM);
    }

    public boolean abrirGaveta(int pino, int tempoInicio, int tempoFim) {
        if (!exigirConexao()) {
            return false;
        }
        intervalo(pino, 0, 1, "Pino");
        intervalo(tempoInicio, 1, 255, "Tempo inicial");
        intervalo(tempoFim, 1, 255, "Tempo final");
        // Usa a alternativa permitida no PDF. NÃO chama AbreGavetaElgin,
        // pois a assinatura fornecida desse outro método diverge da documentação.
        return executarZero("Gaveta", () -> dll.AbreGaveta(pino, tempoInicio, tempoFim));
    }

    public boolean sinalSonoro() {
        return sinalSonoro(QUANTIDADE_BIPS);
    }

    public boolean sinalSonoro(int quantidade) {
        if (!exigirConexao()) {
            return false;
        }
        intervalo(quantidade, 1, 63, "Quantidade de bips");
        return executarZero("Sinal sonoro", () -> dll.SinalSonoro(quantidade, 50, 5));
    }

    public boolean avancarPapel() {
        return avancarPapel(LINHAS_APOS_IMPRESSAO);
    }

    public boolean avancarPapel(int linhas) {
        if (!exigirConexao()) {
            return false;
        }
        intervalo(linhas, 1, 20, "Linhas");
        return executarZero("Avanço de papel", () -> dll.AvancaPapel(linhas));
    }

    public boolean cortarPapel() {
        return cortarPapel(AVANCO_CORTE);
    }

    public boolean cortarPapel(int avanco) {
        if (!exigirConexao()) {
            return false;
        }
        intervalo(avanco, 1, 20, "Avanço antes do corte");
        return executarZero("Corte", () -> dll.Corte(avanco));
    }

    public void consultarPapel() {
        if (!exigirConexao()) {
            return;
        }
        try {
            int retorno = dll.StatusImpressora(3);
            switch (retorno) {
                case 5 -> System.out.println("Papel presente. Retorno: 5.");
                case 6 -> System.out.println("Papel próximo do fim. Retorno: 6.");
                case 7 -> System.out.println("Sem papel. Retorno: 7.");
                default -> {
                    if (retorno < 0) {
                        erro("Status do papel", retorno);
                    } else {
                        System.out.println("Status do papel inesperado: " + retorno + ". Confira o modelo/SDK.");
                    }
                }
            }
        } catch (LinkageError e) {
            falhaNativa(e);
        }
    }

    private static String lerXml(String arquivo) throws IOException {
        Path caminho = Path.of(arquivo).toAbsolutePath().normalize();
        if (!Files.isRegularFile(caminho)) {
            throw new IOException("Arquivo não encontrado: " + caminho);
        }
        if (Files.size(caminho) > 2 * 1024 * 1024) {
            throw new IOException("XML ultrapassa o limite de 2 MiB desta aplicação.");
        }
        String xml = Files.readString(caminho, StandardCharsets.UTF_8);
        return xml.startsWith("\uFEFF") ? xml.substring(1) : xml;
    }

    private static Document validarXml(String xml, String raiz)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        DocumentBuilder leitor = fabrica.newDocumentBuilder();
        leitor.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException e) throws SAXException { throw e; }
            public void error(SAXParseException e) throws SAXException { throw e; }
            public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        Document documento = leitor.parse(new InputSource(new StringReader(xml)));
        if (!raiz.equals(documento.getDocumentElement().getTagName())) {
            throw new IllegalArgumentException("XML errado para esta opção. Raiz esperada: " + raiz);
        }
        // Bem formado não significa validado contra XSD nem assinatura fiscal válida.
        // A validação específica do formato SAT é feita pela DLL.
        return documento;
    }

    private static void intervalo(int valor, int minimo, int maximo, String nome) {
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(nome + " deve estar entre " + minimo + " e " + maximo + ".");
        }
    }

    private static void textoImprimivel(String texto, int maximo, String nome) {
        if (texto == null || texto.isBlank() || texto.length() > maximo
                || texto.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(nome + ": informe de 1 a " + maximo
                    + " caracteres em uma linha, sem controles.");
        }
    }

    private void erro(String operacao, int retorno) {
        System.out.println(operacao + ": falha ou retorno diferente do esperado. Código: " + retorno + ".");
        if (retorno == -4) {
            conexaoAberta = false;
            System.out.println("A DLL informou porta fechada. A conexão local foi marcada como fechada.");
        }
        if (retorno == -21 || retorno == -22 || retorno == -23) {
            System.out.println("Confira energia, cabo, driver USB correto e outros programas usando a impressora.");
        }
        if (retorno == -44 || retorno == -19) {
            System.out.println("Confira a conexão física. Feche e abra a conexão após corrigir.");
        }
        if (operacao.contains("XML") && retorno > 0) {
            System.out.println("Em XML, retorno positivo também pode indicar linha com falha de validação.");
        }
        System.out.println("Confira a saída física antes de repetir: parte dos dados pode ter sido enviada.");
    }

    private static void falhaNativa(LinkageError e) {
        System.out.println("Falha de ligação com DLL/JNA: " + e.getMessage());
        System.out.println("Confira os arquivos e reinicie após corrigir o ambiente.");
    }
}
