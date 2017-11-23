package tr.org.uni_talk.app;

public interface IManager {

    void addManager(IManager manager);

    void onLowMemory();

    void onManagerStart();

    void onManagerLoad();
}
