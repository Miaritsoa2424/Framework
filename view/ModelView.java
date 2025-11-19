package view;

public class ModelView {
    private String view;
    public ModelView(String view) {
        this.setView(view);
    }
    public ModelView() {
    }
    public String getView() {
        return view;
    }
    public void setView(String view) {
        this.view = view;
    }
}
