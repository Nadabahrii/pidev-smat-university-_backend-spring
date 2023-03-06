package com.example.pidev1.service;


import com.example.pidev1.entity.Comment;
import com.example.pidev1.entity.Publication;

import com.example.pidev1.entity.Student;
import com.example.pidev1.repository.Publication_Repository;
import com.example.pidev1.repository.Student_Repository;
import lombok.AllArgsConstructor;
import org.apache.commons.text.similarity.CosineDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class Publication_Service implements IPublication_Service {
    @Autowired
    private Publication_Repository publication_repository;
    @Autowired
    private Student_Repository student_repository;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private ProfanityService profanityService;


    @Override
    public Publication getPublicationById(Long id) {
        return publication_repository.findById(id).orElse(null);
    }

    @Override
    public Publication addPublication(Publication publication) throws IOException {
        if (profanityService.containsProfanity(publication.getContext())) {
            throw new RuntimeException("The publication contains profanity.");
        } else {
            return publication_repository.save(publication);
        }
    }
    @Override
    public List<Publication> retrieveAllPosts() {
        return publication_repository.findAll();
    }





    @Override
    public Publication updatePublication( long idPub, Publication updatedPublication) {
        Optional<Publication> optionalPublication = publication_repository.findById(idPub);
        if (optionalPublication.isPresent()) {
            Publication publication = optionalPublication.get();
            publication.setAuthor(updatedPublication.getAuthor());
            publication.setContext(updatedPublication.getContext());
            publication.setTopic(updatedPublication.getTopic());
            publication.setDate_of_publication(updatedPublication.getDate_of_publication());
            publication.setNbr_likes_Pub(updatedPublication.getNbr_likes_Pub());
            publication.setNbr_dislikes_Pub(updatedPublication.getNbr_dislikes_Pub());
            return publication_repository.save(publication);
        } else {
            return null;
        }
    }
    @Override
    public Publication retrievePublication(Long idPub) {
        Optional<Publication> publication = publication_repository.findById(idPub);
        if (publication.isPresent()) {
            return publication.get();
        } else {
            // handle the case when the publication is not found
            return null;
        }
    }



    @Override
    public void removePost(Long idPub) {
        publication_repository.deleteById(idPub);
    }
    @Override
    public List<Publication> searchSimilarPosts(Publication pub) {
        // Créer un objet CosineDistance
        CosineDistance cosineDistance = new CosineDistance();

        // Récupérer toutes les publications de la base de données
        List<Publication> allPublications = publication_repository.findAll();

        // Créer une liste pour stocker les publications similaires
        List<Publication> similarPublications = new ArrayList<>();

        // Calculer la mesure de similarité cosinus entre chaque publication et la publication donnée
        for (Publication publication : allPublications) {
            double similarity = cosineDistance.apply(pub.getContext(), publication.getContext());

            // Ajouter la publication à la liste des publications similaires si elle est suffisamment similaire
            if (similarity > 0.3 && !publication.equals(pub)) {
                similarPublications.add(publication);
            }
        }

        return similarPublications;
    }
    @Override
    public List<Publication> searchSimilarPublications(Publication pub, int limit) {
        List<Publication> allPublications = publication_repository.findAll();
        List<Publication> similarPublications = new ArrayList<>();

        for (Publication publication : allPublications) {
            int distance = levenshteinDistance(pub.getContext(), publication.getContext());
            if (distance <= 6 && !publication.equals(pub)) {
                similarPublications.add(publication);
            }
            if (similarPublications.size() == limit) {
                break;
            }
        }

        return similarPublications;
    }
    public static int levenshteinDistance(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[m][n];
    }

    @Transactional
    public Publication likePublication(Long idPub, Long studentId) {
        Publication publication = publication_repository.findById(idPub).get();
        if (publication != null) {
            publication.setNbr_likes_Pub(publication.getNbr_likes_Pub() + 1);
            publication.setRating(calculateRating(publication));
            // Add the student's information to the publication
            Student student = student_repository.findById(studentId).get();
            if (student != null) {
                publication.getLikedByStudents().add(student);
            }
            publication_repository.save(publication);
        }
        return publication;
    }
    @Transactional
    public Publication dislikePublication(Long idPub, Long studentId) {
        Publication publication = publication_repository.findById(idPub).get();
        if (publication != null) {
        publication.setNbr_dislikes_Pub(publication.getNbr_dislikes_Pub() + 1);
        publication.setRating(calculateRating(publication));
            Student student = student_repository.findById(studentId).orElse(null);
            if (student != null) {
                publication.getDislikedByStudents().add(student);
            }
            publication_repository.save(publication);
        }
        return publication;
    }






@Override
    public double calculateRating(Publication publication) {
        long likes = publication.getNbr_likes_Pub();
        long dislikes = publication.getNbr_dislikes_Pub();

        double rating = likes / (double) (likes + dislikes);
        publication.setRating(rating);

        return rating;
    }
    @Override
    public List<Publication> getTopRatedPublications() {
        List<Publication> publications = publication_repository.findAll();
        // trier les publications par ordre décroissant de rating
        publications.sort(Comparator.comparingDouble(Publication::getRating).reversed());
        // retourner les 10 premières publications de la liste triée
        return publications.stream().limit(10).collect(Collectors.toList());
    }
    @Override
    public List<Publication> getPublicationsInteractedByStudent(Long studentId) {
        Student student = student_repository.findById(studentId).get();
        List<Publication> interactedPublications = new ArrayList<>();
        List<Publication> publications = publication_repository.findAll();
        if (student == null) {
            return null;
        }
            else {

            for (Publication publication : publications) {
                Set<Comment> comments = publication.getComments();
                for (Comment comment : comments) {
                    if (comment.getStudentss().getIdStudent().equals(studentId)) {
                        interactedPublications.add(publication);
                        break;
                    }
                }
                if (publication.getLikedByStudents().contains(student)) {
                    interactedPublications.add(publication);
                    break;
                }
                if (publication.getDislikedByStudents().contains(student)) {
                    interactedPublications.add(publication);
                    break;
                }
            }

            return interactedPublications;
        }


    }
    @Override
    public Student getMostActiveStudent() {
        List<Student> etudiants = student_repository.findAll();
        Student mostActiveStudent = null;
        int maxInteractions = 0;

        for (Student student : etudiants) {
            List<Publication> interactedPublications = getPublicationsInteractedByStudent(student.getIdStudent());
            int numInteractions = interactedPublications.size();

            if (numInteractions > maxInteractions) {
                mostActiveStudent = student;
                maxInteractions = numInteractions;
            }

        }

        return mostActiveStudent;

}
    @Override
    public void sendSuperFanBadge(Student student) throws MessagingException {

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(student.getEmail());
        helper.setSubject("Félicitations pour votre badge de Super Fan !");
        helper.setText("Bonjour " + student.getFirstname() + ",\n\n"
                + "Félicitations, vous avez été désigné comme notre Super Fan de la semaine en raison de votre activité sur notre plateforme. Nous tenons à vous remercier pour votre engagement et votre soutien.\n\n"
                + "Veuillez trouver ci-joint votre badge de Super Fan.\n\n"
                + "L'équipe DEEPFLOW \n\n"
                + "Cordialement,\n");

        ClassPathResource file = new ClassPathResource("static/images/super_fan_badge.png");
        helper.addAttachment("super_fan_badge.png", file);

        javaMailSender.send(message);
    }





















}



