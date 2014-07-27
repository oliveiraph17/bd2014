package dao;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Usuario;

public class UsuarioDAO extends DAO<Usuario> {

    private static final String createQuery = "INSERT INTO usuario(login, senha, nome, nascimento) VALUES(?, md5(?), ?, ?);";
    private static final String readQuery = "SELECT login, senha, nome, nascimento FROM usuario WHERE id = ?;";
    private static final String updateQuery = "UPDATE usuario SET login = ?, senha = ?, nome = ?, nascimento = ? WHERE id = ?;";
    private static final String deleteQuery = "DELETE FROM usuario WHERE id = ?;";
    private static final String allQuery = "SELECT * FROM usuario;";
    private static final String authenticateQuery = "SELECT id, senha, nome, nascimento FROM usuario WHERE login = ?;";

    UsuarioDAO(Connection connection) {
        super(connection);
    }

    @Override
    public void create(Usuario usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(createQuery);) {
            statement.setString(1, usuario.getLogin());
            statement.setString(2, usuario.getSenha());
            statement.setString(3, usuario.getNome());
            statement.setDate(4, usuario.getNascimento());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw ex;
        }
    }

    @Override
    public Usuario read(Integer id) throws SQLException {
        Usuario usuario = new Usuario();

        try (PreparedStatement statement = connection.prepareStatement(readQuery);) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery();) {
                if (result.next()) {
                    usuario.setId(id);
                    usuario.setLogin(result.getString("login"));
                    usuario.setSenha(result.getString("senha"));
                    usuario.setNome(result.getString("nome"));
                    usuario.setNascimento(result.getDate("nascimento"));
                } else {
                    throw new SQLException("Falha ao visualizar: usuário não encontrado.");
                }
            }
        }

        return usuario;
    }

    @Override
    public void update(Usuario usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(updateQuery);) {
            statement.setString(1, usuario.getLogin());
            statement.setString(2, usuario.getSenha());
            statement.setString(3, usuario.getNome());
            statement.setDate(4, usuario.getNascimento());
            statement.setInt(5, usuario.getId());

            if (statement.executeUpdate() < 1) {
                throw new SQLException("Falha ao editar: usuário não encontrado.");
            }
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(deleteQuery);) {
            statement.setInt(1, id);

            if (statement.executeUpdate() < 1) {
                throw new SQLException("Falha ao excluir: usuário não encontrado.");
            }
        }
    }

    @Override
    public List<Usuario> all() throws SQLException {
        List<Usuario> usuarioList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(allQuery);
             ResultSet result = statement.executeQuery();) {
            while (result.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(result.getInt("id"));
                usuario.setLogin(result.getString("login"));

                usuarioList.add(usuario);
            }
        } catch (SQLException ex) {
            throw ex;
        }

        return usuarioList;
    }

    public void authenticate(Usuario usuario) throws SQLException, SecurityException {
        try (PreparedStatement statement = connection.prepareStatement(authenticateQuery);) {
            statement.setString(1, usuario.getLogin());

            try (ResultSet result = statement.executeQuery();) {
                if (result.next()) {
                    MessageDigest md5;

                    String senhaForm;
                    String senhaUsuario;

                    try {
                        md5 = MessageDigest.getInstance("MD5");
                        md5.update(usuario.getSenha().getBytes());

                        senhaForm = new BigInteger(1, md5.digest()).toString(16);
                        senhaUsuario = result.getString("senha");

                        if (!senhaForm.equals(senhaUsuario)) {
                            throw new SecurityException("Senha incorreta.");
                        }
                    } catch (NoSuchAlgorithmException ex) {
                        System.err.println(ex.getMessage());
                    }

                    usuario.setId(result.getInt("id"));
                    usuario.setNome(result.getString("nome"));
                    usuario.setNascimento(result.getDate("nascimento"));
                } else {
                    throw new SecurityException("Login incorreto.");
                }
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }
}