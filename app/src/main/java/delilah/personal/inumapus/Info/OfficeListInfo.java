package delilah.personal.inumapus.Info;

public class OfficeListInfo {
    public String officeNumber, officeTitle;

    public OfficeListInfo(String number, String title){
        this.officeNumber = number;
        this.officeTitle = title;
    }

    public String getOfficeNumber(){
        return officeNumber;
    }

    public String getOfficeTitle(){
        return officeTitle;
    }
}