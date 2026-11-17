package fr.diginamic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

public class ActeurRepositoryTest {

	private static EntityManagerFactory emf;
	private EntityManager em;

	// ============================================
	// TEST 1 : Tous les acteurs triés par identité
	// ============================================

	@Test
	public void testExtraireActeursTriesParIdentite() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT a FROM Acteur a ORDER BY a.identite ASC",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(1137, acteurs.size());
		assertEquals("A.J. Danna", acteurs.get(0).getIdentite());
	}

	// ============================================
	// TEST 2 : Rechercher Marion Cotillard
	// ============================================

	@Test
	public void testExtraireActeursParIdentite() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT a FROM Acteur a " +
						"WHERE a.identite = 'Marion Cotillard'",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(1, acteurs.size());
		assertEquals("Marion Cotillard",
				acteurs.get(0).getIdentite());
	}

	// ============================================
	// TEST 3 : Acteurs nés en 1985
	// ============================================

	@Test
	public void testExtraireActeursParAnneeNaissance() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT a FROM Acteur a " +
						"WHERE YEAR(a.anniversaire) = 1985",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(10, acteurs.size());
	}

	// ============================================
	// TEST 4 : Actrice ayant joué Harley Quinn
	// ============================================

	@Test
	public void testExtraireActeursParRole() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT DISTINCT a FROM Acteur a " +
						"JOIN a.roles r " +
						"WHERE r.nom = 'Harley Quinn'",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(1, acteurs.size());
		assertEquals("Margot Robbie",
				acteurs.get(0).getIdentite());
	}

	// ============================================
	// TEST 5 : Acteurs de films sortis en 2015
	// ============================================

	@Test
	public void testExtraireActeursParFilmParuAnnee() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT DISTINCT a FROM Acteur a " +
						"JOIN a.roles r " +
						"JOIN r.film f " +
						"WHERE f.annee = 2015",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(119, acteurs.size());
	}

	// ============================================
	// TEST 6 : Acteurs de films français
	// ============================================

	@Test
	public void testExtraireActeursParPays() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT DISTINCT a FROM Acteur a " +
						"JOIN a.roles r " +
						"JOIN r.film f " +
						"JOIN f.pays p " +
						"WHERE p.nom = 'France'",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(158, acteurs.size());
	}

	// ============================================
	// TEST 7 : Films français sortis en 2017
	// ============================================

	@Test
	public void testExtraireActeursParListePaysEtAnnee() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT DISTINCT a FROM Acteur a " +
						"JOIN a.roles r " +
						"JOIN r.film f " +
						"JOIN f.pays p " +
						"WHERE p.nom = 'France' " +
						"AND f.annee = 2017",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(24, acteurs.size());
	}

	// ============================================
	// TEST 8 : Films de Ridley Scott entre
	// 2010 et 2020
	// ============================================

	@Test
	public void testExtraireParRealisateurEntreAnnee() {

		TypedQuery<Acteur> query = em.createQuery(
				"SELECT DISTINCT a FROM Acteur a " +
						"JOIN a.roles r " +
						"JOIN r.film f " +
						"JOIN f.realisateurs real " +
						"WHERE real.identite = 'Ridley Scott' " +
						"AND f.annee BETWEEN 2010 AND 2020",
				Acteur.class
		);

		List<Acteur> acteurs = query.getResultList();

		assertEquals(27, acteurs.size());
	}

	// ============================================
	// TEST 9 : Réalisateurs ayant travaillé
	// avec Brad Pitt
	// ============================================

	@Test
	public void testExtraireRealisateursParActeur() {

		TypedQuery<Realisateur> query = em.createQuery(
				"SELECT DISTINCT real FROM Realisateur real " +
						"JOIN real.films f " +
						"JOIN f.roles r " +
						"JOIN r.acteur a " +
						"WHERE a.identite = 'Brad Pitt'",
				Realisateur.class
		);

		List<Realisateur> realisateurs =
				query.getResultList();

		assertEquals(6, realisateurs.size());
	}

	// ============================================
	// INITIALISATION ET FERMETURE DES TESTS
	// ============================================

	@BeforeEach
	public void ouvertureEm() {
		em = emf.createEntityManager();
	}

	@AfterEach
	public void fermetureEm() {
		em.close();
	}

	@BeforeAll
	public static void initDatabase() {

		emf = Persistence.createEntityManagerFactory("movie_db");

		EntityManager em = emf.createEntityManager();

		try {

			if (em.createQuery("FROM Acteur")
					.getResultList().size() == 0) {

				em.getTransaction().begin();

				Path home = Paths.get(
						ActeurRepositoryTest.class
								.getClassLoader()
								.getResource("data.sql")
								.toURI()
				);

				String[] queries = Files.readAllLines(home)
						.stream()
						.collect(Collectors.joining("\n"))
						.split(";");

				for (String query : queries) {
					em.createNativeQuery(query).executeUpdate();
				}

				em.getTransaction().commit();
			}

		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);

		} finally {
			em.close();
		}
	}

	@AfterAll
	public static void fermetureRessources() {
		emf.close();
	}
}
