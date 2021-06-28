package Client;

import Client.Entities.Animal;
import Client.Entities.Zoo;
import OrmArchivarius.DBManager.ConnectionFactory;
import OrmArchivarius.Manager.OrmManager;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Application {
    public static void main(String[] args) {

        OrmManager ormManager = OrmManager.get("test");
        ormManager.scanPackages(Application.class);
        Zoo zoo1 = new Zoo();
        zoo1.setAddress("zoo address");
        ormManager.save(zoo1);

        Animal animal = new Animal();
        animal.setName("animal");
        animal.setAge(5);
        animal.setZoo(zoo1);
        ormManager.save(animal);

        Animal animal1 = new Animal();
        animal1.setName("animal_1");
        animal1.setAge(6);
        animal1.setZoo(zoo1);
        ormManager.save(animal1);

        System.out.println("animal = " + animal);
        System.out.println("animal1 = " + animal1);

        Long id1 = animal1.getId();
        System.out.println("id of animal1 = " + id1); // >> 2

        animal1.setAge(animal1.getAge() + 1);
        System.out.println("called update");
        ormManager.update(animal1);
        System.out.println("updated age for animal1: " + animal1.getAge()); // >> 7

        Animal animal1_1 = ormManager.getById(Animal.class, id1).orElse(new Animal());
        System.out.println("animal1 from db:" + animal1_1);
        System.out.println(animal1_1.equals(animal1)); // >> true
        System.out.println(animal1_1 == animal1); // >> false

        List<Animal> allAnimals = ormManager.getAll(Animal.class);
        System.out.println("count of animals = " + allAnimals.size()); // >> 2
        System.out.println("animals:" + allAnimals);
        System.out.println(animal.equals(allAnimals.get(0))); // >> true

        System.out.println("delete result: " + ormManager.delete(animal1)); // >> true
        Animal a = ormManager.getById(Animal.class, animal1.getId()).orElse(new Animal());
        System.out.println(a);
        Zoo zoo = ormManager.getById(Zoo.class, zoo1.getId()).orElse(new Zoo());
        System.out.println("zoo from db:\n" + zoo);
        System.out.println("animals from zoo:\n" + zoo.getAnimals());
    }
}