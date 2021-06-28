package Client.Entities;

import OrmArchivarius.Annotations.Column;
import OrmArchivarius.Annotations.Entity;
import OrmArchivarius.Annotations.Id;
import OrmArchivarius.Annotations.OneToMany;
import OrmArchivarius.Manager.OrmManager;

import java.util.ArrayList;
import java.util.List;


@Entity
public class Zoo {
    @Id
    @Column
    Long id;

    @Column
    String address;

    @OneToMany(mappedBy = "id", targetClazz = Animal.class)
    List<Animal> animals = new ArrayList<>();

    public Zoo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Animal> getAnimals() {
        if (animals.isEmpty()) {
            initAnimals();
        }
        return animals;
    }

    public void setAnimals(List<Animal> animals) {
        this.animals = animals;
    }

    private void initAnimals() {
        OrmManager ormManager = OrmManager.get("test");
        animals = ormManager.getAllBy(Animal.class, this);
    }

    @Override
    public String toString() {
        return "Zoo{" +
                "id=" + id +
                ", address='" + address + '\'' +
                '}';
    }
}
