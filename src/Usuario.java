import java.util.Objects;

/** Credenciais de demonstração do trabalho de POO. */
public class Usuario {
    private final String login;
    private final String senha;

    public Usuario(String login, String senha) {
        this.login = Objects.requireNonNull(login);
        this.senha = Objects.requireNonNull(senha);
    }

    public boolean autenticar(String loginInformado, String senhaInformada) {
        return login.equals(loginInformado) && senha.equals(senhaInformada);
    }
}