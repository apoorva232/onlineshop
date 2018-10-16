package Auxiliar;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Razvan
 */
public class DataBase {

    private static Connection con;

    static {
        try {
            con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/magazinonline", "root", "");
        } catch (SQLException ex) {
            Logger.getLogger(DataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//conectare la BD
    public static void connect(String url, String username, String password) throws SQLException {

        con = (Connection) DriverManager.getConnection(url, username, password);
        //con.setAutoCommit(false);
    }
//logare cont

    public static int login(String email, String parola, Boolean[] priv) throws SQLException {

        String sql = "SELECT id_cont, privilegiu FROM conturi "
                + "WHERE email=? AND parola=?";
        PreparedStatement st = con.prepareStatement(sql);
        st.setString(1, email);
        st.setString(2, parola);

        ResultSet rez = st.executeQuery();

        if (rez.next()) {
            if (Integer.parseInt(rez.getString(2)) == 1) {
                priv[0] = true;
            } else {
                priv[0] = false;
            }

            return Integer.parseInt(rez.getString(1));

        }

        return -1;
    }
//creare cont

    public static void signUp(String nume, String prenume, String parola, String email, String dataNastere, String sex, String oras, String adresa) throws SQLException {

        //adauga cont
        String contAdd = "INSERT INTO conturi (nume, prenume, email, parola, sex, privilegiu, data_nastere, oras, adresa) "
                + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        PreparedStatement updtStmt = con.prepareStatement(contAdd);

        updtStmt.setString(1, nume);
        updtStmt.setString(2, prenume);
        updtStmt.setString(3, email);
        updtStmt.setString(4, parola);
        updtStmt.setString(5, sex);
        updtStmt.setInt(6, 0);
        updtStmt.setString(7, dataNastere);
        updtStmt.setString(8, oras);
        updtStmt.setString(9, adresa);

        updtStmt.executeUpdate();

        String updtAdd = "UPDATE conturi SET id_cos_cumparaturi = COALESCE(id_cos_cumparaturi, id_cont);";

        updtStmt = con.prepareStatement(updtAdd);

        updtStmt.executeUpdate();

        //inserare cos_cumparaturi
        String cosAdd = "INSERT INTO cosuri_cumparaturi () VALUES ();";

        PreparedStatement updtStmt1 = con.prepareStatement(cosAdd);
        updtStmt1.executeUpdate();
        String updtAdd1 = "UPDATE cosuri_cumparaturi SET id_lista_produse = COALESCE(id_lista_produse, id_cos_cumparaturi),id_cont = COALESCE(id_cont, id_cos_cumparaturi) ;";

        updtStmt1 = con.prepareStatement(updtAdd1);
        updtStmt1.executeUpdate();

        //inserare lista_cumparaturi
        String listaAdd = "INSERT INTO liste_produse () VALUES ();";

        PreparedStatement updtStmt2 = con.prepareStatement(listaAdd);
        updtStmt2.executeUpdate();
        String updtAdd2 = "UPDATE liste_produse SET id_cos_cumparaturi = COALESCE(id_cos_cumparaturi, id_lista_produse);";

        updtStmt2 = con.prepareStatement(updtAdd2);
        updtStmt2.executeUpdate();
    }

//search
    public static ArrayList<String> search(String criteriu) throws SQLException {

        String sql = "SELECT p.denumire, p.pret, p.stoc, i.link FROM produse p, poze i"
                + " WHERE  p.id_produs = i.id_produs AND (p.denumire LIKE ? OR p.categorie LIKE ?)";

        PreparedStatement getProd = con.prepareStatement(sql);

        criteriu = "%" + criteriu + "%";

        getProd.setString(1, criteriu);
        getProd.setString(2, criteriu);

        ResultSet rez = getProd.executeQuery();

        ArrayList<String> prodArray = new ArrayList<>();

        while (rez.next()) {
            prodArray.add(rez.getString(4) + " " + rez.getString(2) + " " + rez.getString(3) + " " + rez.getString(1));
        }

        return prodArray;

    }

    //returns all products
    public static ArrayList<String> returnProducts() throws SQLException {

        String sql = "SELECT p.denumire, p.pret, p.stoc, i.link FROM produse p, poze i"
                + " WHERE  p.id_produs = i.id_produs ;";

        PreparedStatement getProd = con.prepareStatement(sql);

        ResultSet rez = getProd.executeQuery();

        ArrayList<String> prodArray = new ArrayList<>();

        while (rez.next()) {
            prodArray.add(rez.getString(4) + " " + rez.getString(2) + " " + rez.getString(3) + " " + rez.getString(1));
        }

        return prodArray;

    }

    //adaugare in cos
    public static boolean addCart(String numeProd, int id, int cantitate) throws SQLException {

        String sql1 = "SELECT id_produs, stoc, pret FROM produse"
                + " WHERE denumire = ?;";

        PreparedStatement addProd = con.prepareStatement(sql1);

        addProd.setString(1, numeProd);

        ResultSet rez1 = addProd.executeQuery();

        rez1.next();

        if (Integer.parseInt(rez1.getString(2)) < cantitate) {
            return false;
        }
        // OBTIN ID LISTA CARE TREBUIE MODIFICATA
        String sql2 = "SELECT l.id_lista_produse FROM liste_produse l, conturi c"
                + " WHERE c.id_cont = l.id_cos_cumparaturi  AND c.id_cont = ?;";

        addProd = con.prepareStatement(sql2);
        addProd.setInt(1, id);
        ResultSet rez2 = addProd.executeQuery();

        rez2.next();

        String sql4 = "SELECT COUNT(id_cantitate), id_cantitate"
                + " FROM cantitati"
                + " WHERE id_produs = ? AND id_lista_produse = ?";

        PreparedStatement stmt4 = con.prepareStatement(sql4);

        stmt4.setInt(1, Integer.parseInt(rez1.getString(1)));
        stmt4.setInt(2, Integer.parseInt(rez2.getString(1)));

        ResultSet rez4 = stmt4.executeQuery();

        rez4.next();

        if (Integer.parseInt(rez4.getString(1)) == 0) {
            String sql3 = "INSERT INTO cantitati (cantitate_produs, id_lista_produse, id_produs )"
                    + " VALUES (? , ?, ?);";

            addProd = con.prepareStatement(sql3);
            addProd.setInt(1, cantitate);
            addProd.setString(2, rez2.getString(1));
            addProd.setString(3, rez1.getString(1));

            addProd.executeUpdate();

        } else {

            String sql5 = "UPDATE cantitati"
                    + " SET cantitate_produs = cantitate_produs + ?"
                    + " WHERE id_cantitate = ?";

            PreparedStatement stmt5 = con.prepareStatement(sql5);

            stmt5.setInt(1, cantitate);
            stmt5.setInt(2, Integer.parseInt(rez4.getString(2)));

            stmt5.executeUpdate();

        }
        String sql6 = "UPDATE liste_produse"
                + " SET valoare = valoare + ?"
                + " WHERE id_lista_produse = ?";

        PreparedStatement stmt6 = con.prepareStatement(sql6);

        stmt6.setInt(1, Integer.parseInt(rez1.getString(3)) * cantitate);
        stmt6.setInt(2, Integer.parseInt(rez2.getString(1)));

        stmt6.executeUpdate();

        return true;
    }

//arata date cont
    public static String showAccDet(int idUser) throws SQLException {

//returnez tot ce este in cont
        String sql = "SELECT * FROM conturi WHERE id_cont = ?;";
        PreparedStatement st = con.prepareStatement(sql);
        st.setInt(1, idUser);

        ResultSet rez = st.executeQuery();
        //System.out.println(rez.getString(1));
        rez.next();
        return rez.getString(1) + " " + rez.getString(3) + " " + rez.getString(2) + " "
                + rez.getString(4) + " " + rez.getString(5) + " " + rez.getString(6) + " "
                + rez.getString(8) + " " + rez.getString(9) + " " + rez.getString(10);

    }
//editare fielduri user

    public static void editUser(String nume, String prenume, String parola, String email, String dataNastere, String sex, String oras, String adresa, int id_cont) throws SQLException {

        String sql = "UPDATE conturi SET nume = ?, prenume = ?, parola = ?,"
                + " email = ?, data_nastere = ?, sex = ?, oras = ?, adresa = ?"
                + " WHERE id_cont = ?;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, nume);
        stmt.setString(2, prenume);
        stmt.setString(3, parola);
        stmt.setString(4, email);
        stmt.setString(5, dataNastere);
        stmt.setString(6, sex);
        stmt.setString(7, oras);
        stmt.setString(8, adresa);
        stmt.setInt(9, id_cont);

        stmt.executeUpdate();

    }

    //adauga produs
    public static void addProd(String denumire, double pret, int stoc, String categorie) throws SQLException {

//verific daca exista deja si adaug la stoc, altfel, adaug un nou produs
        String sq = "SHOW TABLE STATUS LIKE 'poze'";
        PreparedStatement stmt4 = con.prepareStatement(sq);
        ResultSet rez4 = stmt4.executeQuery(sq);
        rez4.next();

        int idPoza = Integer.parseInt(rez4.getString(11));

        String sql = "SELECT COUNT(id_produs) FROM produse "
                + " WHERE denumire  = ?;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, denumire);

        ResultSet rez = stmt.executeQuery();
        rez.next();
        if (Integer.parseInt(rez.getString(1)) != 0) {

            String sql2 = "UPDATE produse "
                    + "SET stoc =  stoc + ? "
                    + "WHERE denumire  = ?;";

            PreparedStatement stmt1 = con.prepareStatement(sql2);
            stmt1.setInt(1, stoc);
            stmt1.setString(2, denumire);
            stmt1.executeUpdate();

        } else {

            String sql3 = "INSERT INTO produse (denumire, pret, stoc, categorie, id_poza)"
                    + " VALUES(?, ?, ?, ?, ? );";

            PreparedStatement stmt3 = con.prepareStatement(sql3);

            stmt3.setString(1, denumire);
            stmt3.setDouble(2, pret);
            stmt3.setInt(3, stoc);
            stmt3.setString(4, categorie);
            stmt3.setInt(5, idPoza);

            stmt3.executeUpdate();

        }

    }

    //adaugare poza
    public static void addPicture(String denumireProd, String link) throws SQLException {

        String sql = "SELECT id_produs FROM produse"
                + " WHERE denumire = ?;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, denumireProd);

        ResultSet rez = stmt.executeQuery();

        rez.next();

        int id = Integer.parseInt(rez.getString(1));

        String sq = "SELECT COUNT(id_poza) "
                + "FROM poze "
                + "WHERE id_produs = ? ;";

        PreparedStatement st = con.prepareStatement(sq);
        st.setInt(1, id);
        ResultSet r = st.executeQuery();

        r.next();
        if (Integer.parseInt(r.getString(1)) == 0) {
            String sql1 = "INSERT INTO poze (link, id_produs) "
                    + " VALUES( ?, ?)";

            PreparedStatement stmt1 = con.prepareStatement(sql1);

            stmt1.setString(1, link);
            stmt1.setInt(2, id);

            stmt1.executeUpdate();
        }

    }

    public static ArrayList<String> showCart(int idCont) throws SQLException {

        String sql = "SELECT id_lista_produse, valoare FROM liste_produse "
                + "WHERE id_cos_cumparaturi = ?;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, idCont);

        ResultSet rez = stmt.executeQuery();

        rez.next();

        int idLista = Integer.parseInt(rez.getString(1));

        String sql1 = "SELECT c.cantitate_produs, p.denumire, p.pret FROM cantitati c, produse p"
                + " WHERE c.id_lista_produse = ? AND c.id_produs = p.id_produs";

        stmt = con.prepareStatement(sql1);

        stmt.setInt(1, idLista);

        ResultSet rez1 = stmt.executeQuery();

        ArrayList<String> showC = new ArrayList<>();

        while (rez1.next()) {
            showC.add(rez1.getString(1) + " " + rez1.getString(3) + " " + rez1.getString(2));
        }

        //showC.add(rez.getString(2));
        return showC;
    }

    public static int getAccNo() throws SQLException {

        String sql = " SELECT COUNT(id_cont)"
                + "FROM conturi";

        PreparedStatement stmt = con.prepareStatement(sql);

        ResultSet rez = stmt.executeQuery();

        rez.next();
        return Integer.parseInt(rez.getString(1));

    }

    public static void deleteFromCart(String denProd, int idCont) throws SQLException {

        String sql = "SELECT id_lista_produse FROM liste_produse "
                + "WHERE id_cos_cumparaturi = ?;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, idCont);

        ResultSet rez = stmt.executeQuery();

        rez.next();
//OBTIN IDLISTAPRODUSE
        int idLista = Integer.parseInt(rez.getString(1));

        String sq = "SELECT pret, id_produs FROM produse "
                + " WHERE denumire = ?";

        PreparedStatement st = con.prepareStatement(sq);

        st.setString(1, denProd);

        ResultSet re = st.executeQuery();

        re.next();

        double pretProd = Double.parseDouble(re.getString(1));

        String s = "SELECT cantitate_produs FROM cantitati "
                + " WHERE id_lista_produse = ?";

        PreparedStatement q = con.prepareStatement(s);

        q.setInt(1, idLista);

        ResultSet r = q.executeQuery();

        r.next();

        int cantitate = Integer.parseInt(r.getString(1));

        String sql1 = "DELETE FROM cantitati "
                + "WHERE id_lista_produse = ? AND id_produs = ?;";

        PreparedStatement stmt1 = con.prepareStatement(sql1);

        stmt1.setInt(1, idLista);
        stmt1.setInt(2, Integer.parseInt(re.getString(2)));

        stmt1.executeUpdate();

        String sql6 = "UPDATE liste_produse"
                + " SET valoare = valoare - ?"
                + " WHERE id_lista_produse = ?";

        PreparedStatement stmt6 = con.prepareStatement(sql6);

        stmt6.setDouble(1, pretProd * cantitate);
        stmt6.setInt(2, idLista);

        stmt6.executeUpdate();

    }

    public static void doComanda(int IDUser) throws SQLException {

        String date = "SELECT date(SYSDATE()) " //obtin data
                + "FROM DUAL;";

        PreparedStatement stmt = con.prepareStatement(date);

        ResultSet rez = stmt.executeQuery();

        rez.next();

        String data = rez.getString(1);

        String sql2 = "SELECT id_lista_produse FROM liste_produse" //obtin id_lista_produse
                + " WHERE id_cos_cumparaturi = ?";

        PreparedStatement stmt2 = con.prepareStatement(sql2);

        stmt2.setInt(1, IDUser);

        ResultSet rez2 = stmt2.executeQuery();

        rez2.next();

        int IDLista = Integer.parseInt(rez2.getString(1));

        String sql1 = "INSERT INTO comenzi (data, id_cont, id_lista_produse)" //creez comanda
                + " VALUES(?, ?, ?);";

        PreparedStatement stmt1 = con.prepareStatement(sql1);

        stmt1.setString(1, data);
        stmt1.setInt(2, IDUser);
        stmt1.setInt(3, IDLista);

        stmt1.executeUpdate();

//obtine id_comanda
        String sq = "SHOW TABLE STATUS LIKE 'comenzi'";
        PreparedStatement stmt3 = con.prepareStatement(sq);
        ResultSet rez4 = stmt3.executeQuery(sq);
        rez4.next();

        int idComanda = Integer.parseInt(rez4.getString(11)) - 1;

        //sterge id_cos_cumparaturi din lista si se pune id_comanda
        String sql4 = "UPDATE liste_produse"
                + " SET id_cos_cumparaturi = NULL, id_comanda = ? "
                + " WHERE id_lista_produse = ?;";

        PreparedStatement stmt4 = con.prepareStatement(sql4);

        stmt4.setInt(1, idComanda);
        stmt4.setInt(2, IDLista);

        stmt4.executeUpdate();

        //creaza lista noua pt co de cump
        String sql5 = "INSERT INTO liste_produse (id_cos_cumparaturi)"
                + " VALUES(?);";

        PreparedStatement stmt5 = con.prepareStatement(sql5);

        stmt5.setInt(1, IDUser);

        stmt5.executeUpdate();

        //SCADE CANTITATE DIN STOC
        String sql7 = "SELECT c.cantitate_produs, p.id_produs"
                + " FROM cantitati c,produse p"
                + " WHERE c.id_lista_produse = ? AND p.id_produs = c.id_produs ";

        PreparedStatement stmt7 = con.prepareStatement(sql7);

        stmt7.setInt(1, IDLista);

        ResultSet rez7 = stmt7.executeQuery();

        while (rez7.next()) {

            String sql6 = "UPDATE produse "
                    + "SET stoc = stoc - ? "
                    + "WHERE id_produs = ?";

            PreparedStatement stmt6 = con.prepareStatement(sql6);

            stmt6.setInt(1, Integer.parseInt(rez7.getString(1)));
            stmt6.setInt(2, Integer.parseInt(rez7.getString(2)));

            stmt6.executeUpdate();

        }

        String ssq1 = "SHOW TABLE STATUS LIKE 'liste_produse'";

        PreparedStatement sst1 = con.prepareStatement(ssq1);

        ResultSet rr1 = sst1.executeQuery();
        rr1.next();

        int IDListaNoua = Integer.parseInt(rr1.getString(11)) - 1;

        String ssq = "UPDATE cosuri_cumparaturi  "
                + " SET id_lista_produse = ? WHERE id_cos_cumparaturi = ? ";

        PreparedStatement sst = con.prepareStatement(ssq);

        sst.setInt(1, IDListaNoua);
        sst.setInt(2, IDUser);

        sst.executeUpdate();

    }

    public static ArrayList<String> getCategories() throws SQLException {

        ArrayList<String> arr = new ArrayList<>();

        String sql = "SELECT DISTINCT categorie FROM produse";

        PreparedStatement stmt = con.prepareStatement(sql);

        ResultSet rez = stmt.executeQuery();

        while (rez.next()) {
            arr.add(rez.getString(1));
        }

        return arr;

    }

    public static ArrayList<String> raportProdus(String denProd) throws SQLException {

        String sql = "SELECT  p.denumire, SUM(c.cantitate_produs), SUM(p.pret * c.cantitate_produs)"
                + " FROM produse p, cantitati c"
                + " WHERE p.denumire = ?  AND c.id_produs = p.id_produs";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, denProd);

        ResultSet rez = stmt.executeQuery();

        ArrayList<String> rapProd = new ArrayList<>();
        while (rez.next()) {
            rapProd.add(rez.getString(2) + " " + rez.getString(3) + " " + rez.getString(1));
        }

        return rapProd;
    }

    public static ArrayList<String> raportCont(int idCont) throws SQLException {

        ArrayList<String> userReport = new ArrayList<>();

        String sql = "SELECT u.id_cont, c.id_comanda, c.data, l.valoare FROM comenzi c, liste_produse l, conturi u"
                + " WHERE u.id_cont = ? AND c.id_cont = u.id_cont AND l.id_lista_produse = c.id_lista_produse";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, idCont);
        ResultSet rez = stmt.executeQuery();

        while (rez.next()) {
            userReport.add(rez.getString(1) + " " + rez.getString(2) + " " + rez.getString(4) + " " + rez.getString(3));
        }

        return userReport;

    }

    public static ArrayList<String> raportCategorie(String categorie) throws SQLException {

        String sql = "SELECT p.categorie, SUM(c.cantitate_produs ) AS 'bani/categorie' FROM cantitati c, produse p"
                + " WHERE p.categorie = ? AND c.id_produs = p.id_produs"
                + " GROUP BY p.categorie;";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, categorie);

        ResultSet rez = stmt.executeQuery();

        ArrayList<String> rapCat = new ArrayList<>();

        while (rez.next()) {
            rapCat.add(rez.getString(2));
        }

        return rapCat;

    }

}
