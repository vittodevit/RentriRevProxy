package app.fiuto.rentrirevproxy.model;

public class CumulativeResponse {

    RentriCheckResponse responseAnagrafiche;
    RentriCheckResponse responseCaRentri;
    RentriCheckResponse responseCodifiche;
    RentriCheckResponse responseDatiRegistri;
    RentriCheckResponse responseFormulari;
    RentriCheckResponse responseVidimazioneFormulari;

    public CumulativeResponse() {
        this.responseAnagrafiche = new RentriCheckResponse(false, "Anagrafiche not checked");
        this.responseCaRentri = new RentriCheckResponse(false, "CaRentri not checked");
        this.responseCodifiche = new RentriCheckResponse(false, "Codifiche not checked");
        this.responseDatiRegistri = new RentriCheckResponse(false, "DatiRegistri not checked");
        this.responseFormulari = new RentriCheckResponse(false, "Formulari not checked");
        this.responseVidimazioneFormulari = new RentriCheckResponse(false, "VidimazioneFormulari not checked");
    }

    public RentriCheckResponse getResponseAnagrafiche() {
        return responseAnagrafiche;
    }

    public void setResponseAnagrafiche(RentriCheckResponse responseAnagrafiche) {
        this.responseAnagrafiche = responseAnagrafiche;
    }

    public RentriCheckResponse getResponseCaRentri() {
        return responseCaRentri;
    }

    public void setResponseCaRentri(RentriCheckResponse responseCaRentri) {
        this.responseCaRentri = responseCaRentri;
    }

    public RentriCheckResponse getResponseCodifiche() {
        return responseCodifiche;
    }

    public void setResponseCodifiche(RentriCheckResponse responseCodifiche) {
        this.responseCodifiche = responseCodifiche;
    }

    public RentriCheckResponse getResponseDatiRegistri() {
        return responseDatiRegistri;
    }

    public void setResponseDatiRegistri(RentriCheckResponse responseDatiRegistri) {
        this.responseDatiRegistri = responseDatiRegistri;
    }

    public RentriCheckResponse getResponseFormulari() {
        return responseFormulari;
    }

    public void setResponseFormulari(RentriCheckResponse responseFormulari) {
        this.responseFormulari = responseFormulari;
    }

    public RentriCheckResponse getResponseVidimazioneFormulari() {
        return responseVidimazioneFormulari;
    }

    public void setResponseVidimazioneFormulari(RentriCheckResponse responseVidimazioneFormulari) {
        this.responseVidimazioneFormulari = responseVidimazioneFormulari;
    }

}
