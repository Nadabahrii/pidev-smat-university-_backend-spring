package com.example.pidev1.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table( name = "Student")
public class Student implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idStudent")
    private Long idStudent;
    private String Firstname;
    private String Lastname;
    private String Email;
    private String Address;
    private String Contact;
    private Long Age;
    private String hobby;

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Class> classSet;
    @JsonIgnore
    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Department> departments;
    @JsonIgnore
    @OneToOne(mappedBy="student")
    private Request requests;
    @JsonIgnore
    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Event> events;
    @JsonIgnore
    @ManyToOne
    Host host;

    @ManyToOne
    Bus bus;
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy="students")
    private Set<Publication> publications;
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy="studentss")
    private Set<Comment> comments;
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy="studentt")
    private Set<Rdv> rdvs;



}
