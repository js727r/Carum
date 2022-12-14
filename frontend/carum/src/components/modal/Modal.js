import React from "react";
import "./Modal.css";
import CloseIcon from "@mui/icons-material/Close";

const Modal = (props) => {
  const { open, close, header } = props;

  return (
    <div className={open ? "openModal modal" : "modal"}>
      {open ? (
        <section>
          <header>
            <div>{header}</div>
            <div>
              <button className="close" onClick={close}>
                <CloseIcon />
              </button>
            </div>
          </header>
          <main>{props.children}</main>
          <footer>
            {/* <button className="close" onClick={close}>
                close
              </button> */}
          </footer>
        </section>
      ) : null}
    </div>
  );
};

export default Modal;
